package com.portfoliopilot.service;

import com.portfoliopilot.dto.AnalysisDto;
import com.portfoliopilot.entity.AssetPrice;
import com.portfoliopilot.entity.FinPortfolio;
import com.portfoliopilot.entity.Holding;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AssetPriceRepository;
import com.portfoliopilot.repository.FinPortfolioRepository;
import com.portfoliopilot.repository.HoldingRepository;
import com.portfoliopilot.repository.UserRepository;
import com.portfoliopilot.service.MetricsService.PricePoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Assembles the full quantitative report: valuation at latest prices,
 * return/volatility/Sharpe/drawdown over the forward-filled history,
 * allocation + HHI concentration, and benchmark comparison on aligned
 * dates. Every section degrades to an explained null — never NaN.
 */
@Service
public class AnalysisService {

    private final FinPortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final AssetPriceRepository priceRepository;
    private final UserRepository userRepository;

    public AnalysisService(FinPortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            AssetPriceRepository priceRepository,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
    }

    public AnalysisDto analyze(String email, Long portfolioId) {
        return analyze(email, portfolioId, null, null);
    }

    /**
     * Same report restricted to an optional inclusive date window. The window
     * filters the valued curve (returns, drawdown, benchmark alignment, and
     * the returned series); valuation stays point-in-time at latest prices.
     * Blank bounds are ignored; start after end is a 400.
     */
    public AnalysisDto analyze(String email, Long portfolioId, LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date must be on or before end date.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        FinPortfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        List<Holding> holdings = holdingRepository.findByPortfolioIdOrderBySymbol(portfolio.getId());
        List<String> warnings = new ArrayList<>();

        Map<String, Map<LocalDate, Double>> histories = new TreeMap<>();
        for (Holding h : holdings) {
            List<AssetPrice> rows = priceRepository.findByUserIdAndSymbolOrderByPriceDate(user.getId(), h.getSymbol());
            Map<LocalDate, Double> byDate = new TreeMap<>();
            for (AssetPrice row : rows) {
                byDate.put(row.getPriceDate(), row.getClose());
            }
            histories.put(h.getSymbol(), byDate);
        }

        AnalysisDto.Valuation valuation = valuate(holdings, histories, warnings);
        List<PricePoint> curve = portfolioCurve(holdings, histories);
        if (start != null || end != null) {
            curve = curve.stream()
                    .filter(p -> (start == null || !p.date().isBefore(start))
                            && (end == null || !p.date().isAfter(end)))
                    .toList();
            if (curve.isEmpty()) {
                warnings.add("No valued dates between "
                        + (start == null ? "the beginning" : start)
                        + " and " + (end == null ? "the latest date" : end) + ".");
            }
        }
        Map<LocalDate, Double> benchmarkHistory =
                benchmarkHistory(user.getId(), portfolio.getBenchmarkSymbol());
        AnalysisDto.ReturnsStats returns = returns(curve, warnings);
        AnalysisDto.Allocation allocation = allocate(valuation);
        AnalysisDto.BenchmarkComparison benchmark =
                compareBenchmark(portfolio.getBenchmarkSymbol(), benchmarkHistory, curve, warnings);
        List<AnalysisDto.SeriesPoint> series = curve.stream()
                .map(p -> new AnalysisDto.SeriesPoint(p.date().toString(), p.close(),
                        benchmarkHistory.get(p.date())))
                .toList();

        return new AnalysisDto(portfolio.getId(), portfolio.getName(), valuation, returns,
                allocation, benchmark, warnings, series);
    }

    private AnalysisDto.Valuation valuate(List<Holding> holdings,
            Map<String, Map<LocalDate, Double>> histories, List<String> warnings) {
        List<AnalysisDto.Position> positions = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double total = 0.0;
        LocalDate asOf = null;

        for (Holding h : holdings) {
            Map<LocalDate, Double> history = histories.getOrDefault(h.getSymbol(), Map.of());
            if (history.isEmpty()) {
                missing.add(h.getSymbol());
                positions.add(new AnalysisDto.Position(h.getSymbol(), h.getAssetCategory(),
                        h.getQuantity(), null, null, 0.0, 0.0));
                continue;
            }
            LocalDate lastDate = ((TreeMap<LocalDate, Double>) history).lastKey();
            Double lastPrice = history.get(lastDate);
            double value = h.getQuantity() * lastPrice;
            if (asOf == null || lastDate.isAfter(asOf)) {
                asOf = lastDate;
            }
            total += value;
            positions.add(new AnalysisDto.Position(h.getSymbol(), h.getAssetCategory(),
                    h.getQuantity(), lastPrice, lastDate.toString(), value, 0.0));
        }
        if (!missing.isEmpty()) {
            warnings.add("No prices for: " + String.join(", ", missing) + " — import history to value them.");
        }
        List<AnalysisDto.Position> weighted = new ArrayList<>();
        for (AnalysisDto.Position p : positions) {
            double weight = total > 0 ? p.value() / total : 0.0;
            weighted.add(new AnalysisDto.Position(p.symbol(), p.assetCategory(), p.quantity(),
                    p.lastPrice(), p.lastDate(), p.value(), weight));
        }
        return new AnalysisDto.Valuation(asOf == null ? null : asOf.toString(),
                holdings.isEmpty() ? 0.0 : total, weighted, missing);
    }

    /** Portfolio value per date using last-observation-carried-forward prices. */
    static List<PricePoint> portfolioCurve(List<Holding> holdings,
            Map<String, Map<LocalDate, Double>> histories) {
        List<LocalDate> calendar = MetricsService.unionCalendar(histories);
        Map<LocalDate, Double> curve = new TreeMap<>();
        for (LocalDate date : calendar) {
            double value = 0.0;
            boolean any = false;
            for (Holding h : holdings) {
                Double close = latestOnOrBefore(histories.get(h.getSymbol()), date);
                if (close != null) {
                    value += h.getQuantity() * close;
                    any = true;
                }
            }
            if (any && value > 0) {
                curve.put(date, value);
            }
        }
        return curve.entrySet().stream()
                .map(e -> new PricePoint(e.getKey(), e.getValue())).toList();
    }

    private AnalysisDto.ReturnsStats returns(List<PricePoint> curve, List<String> warnings) {
        if (curve.size() < 2) {
            return new AnalysisDto.ReturnsStats(0, null, null, null, null, null, null, null,
                    "Need at least 2 valued dates — add holdings and import more price history.");
        }
        List<Double> rets = MetricsService.dailyReturns(curve);
        Double cumulative = MetricsService.cumulativeReturn(curve);
        Double annualized = MetricsService.annualizedReturn(cumulative, rets.size());
        Double vol = MetricsService.annualizedVolatility(rets);
        Double sharpe = MetricsService.sharpeRatio(rets, vol);
        Double dd = MetricsService.maxDrawdown(curve);
        if (rets.size() < MetricsService.ROBUST_OBSERVATIONS) {
            warnings.add("Only " + rets.size() + " return observations — volatility and Sharpe are indicative, not robust.");
        }
        if (vol != null && vol == 0) {
            warnings.add("Zero volatility (flat series) — Sharpe ratio is not meaningful.");
        }
        return new AnalysisDto.ReturnsStats(rets.size(),
                curve.get(0).date().toString(), curve.get(curve.size() - 1).date().toString(),
                cumulative, annualized, vol, sharpe, dd, null);
    }

    private AnalysisDto.Allocation allocate(AnalysisDto.Valuation valuation) {
        Map<String, Double> weights = new LinkedHashMap<>();
        for (AnalysisDto.Position p : valuation.positions()) {
            weights.put(p.symbol(), p.weight());
        }
        Double hhi = MetricsService.hhi(weights);
        return new AnalysisDto.Allocation(weights, hhi, MetricsService.hhiBand(hhi));
    }

    /** Benchmark closes by date (empty when unset or unimported). */
    private Map<LocalDate, Double> benchmarkHistory(Long userId, String benchmarkSymbol) {
        if (benchmarkSymbol == null || benchmarkSymbol.isBlank()) {
            return Map.of();
        }
        String symbol = benchmarkSymbol.trim().toUpperCase();
        List<AssetPrice> rows = priceRepository.findByUserIdAndSymbolOrderByPriceDate(userId, symbol);
        Map<LocalDate, Double> byDate = new TreeMap<>();
        for (AssetPrice row : rows) {
            byDate.put(row.getPriceDate(), row.getClose());
        }
        return byDate;
    }

    /**
     * Benchmark comparison on dates shared by both series: cumulative and
     * annualized returns over the identical window, active return, and
     * tracking error from paired daily differences.
     */
    private AnalysisDto.BenchmarkComparison compareBenchmark(String benchmarkSymbol,
            Map<LocalDate, Double> benchByDate, List<PricePoint> curve, List<String> warnings) {
        if (benchmarkSymbol == null || benchmarkSymbol.isBlank()) {
            return new AnalysisDto.BenchmarkComparison(null, 0, null, null, null, null,
                    "Set a benchmark symbol on the portfolio to enable comparison.");
        }
        String symbol = benchmarkSymbol.trim().toUpperCase();
        if (benchByDate.size() < 2) {
            warnings.add("No price history for benchmark " + symbol + " — import it to compare.");
            return new AnalysisDto.BenchmarkComparison(symbol, 0, null, null, null, null,
                    "Import price history for " + symbol + " to enable comparison.");
        }
        Map<LocalDate, Double> portByDate = MetricsService.toDateMap(curve);
        List<LocalDate> shared = portByDate.keySet().stream()
                .filter(benchByDate::containsKey).sorted().toList();
        if (shared.size() < 2) {
            warnings.add("Benchmark " + symbol + " shares fewer than 2 dates with the portfolio — import overlapping history.");
            return new AnalysisDto.BenchmarkComparison(symbol, 0, null, null, null, null,
                    "Need at least 2 overlapping dates with " + symbol + ".");
        }
        List<PricePoint> portWindow = shared.stream()
                .map(d -> new PricePoint(d, portByDate.get(d))).toList();
        List<PricePoint> benchWindow = shared.stream()
                .map(d -> new PricePoint(d, benchByDate.get(d))).toList();
        List<Double> portRets = MetricsService.dailyReturns(portWindow);
        List<Double> benchRets = MetricsService.dailyReturns(benchWindow);
        Double portCum = MetricsService.cumulativeReturn(portWindow);
        Double benchCum = MetricsService.cumulativeReturn(benchWindow);
        Double benchAnn = MetricsService.annualizedReturn(benchCum, benchRets.size());
        Double active = (portCum != null && benchCum != null) ? portCum - benchCum : null;
        List<Double> diffs = new ArrayList<>();
        for (int i = 0; i < Math.min(portRets.size(), benchRets.size()); i++) {
            diffs.add(portRets.get(i) - benchRets.get(i));
        }
        Double trackingError = MetricsService.trackingError(diffs);
        if (portRets.size() < MetricsService.ROBUST_OBSERVATIONS) {
            warnings.add("Comparison window has only " + portRets.size()
                    + " paired observations — treat active return and tracking error as indicative.");
        }
        String period = shared.get(0) + " → " + shared.get(shared.size() - 1);
        return new AnalysisDto.BenchmarkComparison(symbol, portRets.size(), benchCum, benchAnn,
                active, trackingError, "Compared over identical window " + period + ".");
    }

    private static Double latestOnOrBefore(Map<LocalDate, Double> history, LocalDate date) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        Double best = null;
        for (Map.Entry<LocalDate, Double> e : history.entrySet()) {
            if (!e.getKey().isAfter(date) && e.getValue() != null && e.getValue() > 0) {
                best = e.getValue();
            }
        }
        return best;
    }
}
