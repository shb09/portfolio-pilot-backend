package com.portfoliopilot.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure quantitative engine (no Spring, no DB): simple daily returns,
 * annualization over 252 trading days, sample volatility, Sharpe at zero
 * risk-free rate, maximum drawdown, HHI concentration.
 *
 * Documented assumptions: prices sorted ascending with no gaps assumed —
 * callers forward-fill first; every method returns null (never NaN or
 * Infinity) when its minimum data is missing.
 */
public final class MetricsService {

    /** Trading days per year used for every annualization. */
    public static final int TRADING_DAYS = 252;
    /** Volatility below ~30 annualized observations is reported but flagged. */
    public static final int ROBUST_OBSERVATIONS = 30;

    private MetricsService() {
    }

    public record PricePoint(LocalDate date, double close) {
    }

    /** Simple returns r_t = P_t / P_{t-1} - 1. Needs >= 2 prices; else empty. */
    public static List<Double> dailyReturns(List<PricePoint> prices) {
        List<Double> out = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            double prev = prices.get(i - 1).close();
            double cur = prices.get(i).close();
            if (prev <= 0 || cur <= 0 || !Double.isFinite(prev) || !Double.isFinite(cur)) {
                continue;
            }
            out.add(cur / prev - 1.0);
        }
        return out;
    }

    /** Cumulative return over the series; null when < 2 prices. */
    public static Double cumulativeReturn(List<PricePoint> prices) {
        if (prices.size() < 2) {
            return null;
        }
        double first = prices.get(0).close();
        double last = prices.get(prices.size() - 1).close();
        if (first <= 0 || !Double.isFinite(first) || !Double.isFinite(last)) {
            return null;
        }
        return last / first - 1.0;
    }

    /** Annualized from cumulative over n return-observations; null when n < 1. */
    public static Double annualizedReturn(Double cumulative, int returnObservations) {
        if (cumulative == null || returnObservations < 1 || !Double.isFinite(cumulative)) {
            return null;
        }
        double base = 1.0 + cumulative;
        if (base <= 0) {
            return null;
        }
        double value = Math.pow(base, (double) TRADING_DAYS / returnObservations) - 1.0;
        return Double.isFinite(value) ? value : null;
    }

    /** Annualized sample volatility (ddof=1); null when < 2 returns or zero variance input invalid. */
    public static Double annualizedVolatility(List<Double> returns) {
        Double std = sampleStd(returns);
        if (std == null) {
            return null;
        }
        double value = std * Math.sqrt(TRADING_DAYS);
        return Double.isFinite(value) ? value : null;
    }

    /**
     * Annualized Sharpe at zero risk-free rate; null when volatility is
     * missing or zero (flat series has no meaningful Sharpe).
     */
    public static Double sharpeRatio(List<Double> returns, Double annualizedVol) {
        if (returns == null || returns.isEmpty() || annualizedVol == null || annualizedVol <= 0) {
            return null;
        }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (!Double.isFinite(mean)) {
            return null;
        }
        double value = mean * TRADING_DAYS / annualizedVol;
        return Double.isFinite(value) ? value : null;
    }

    /** Maximum peak-to-trough decline of the equity curve; null when < 2 prices. */
    public static Double maxDrawdown(List<PricePoint> prices) {
        if (prices.size() < 2) {
            return null;
        }
        double first = prices.get(0).close();
        if (first <= 0 || !Double.isFinite(first)) {
            return null;
        }
        double peak = 1.0;
        double worst = 0.0;
        for (PricePoint p : prices) {
            double level = p.close() / first;
            if (!Double.isFinite(level) || level <= 0) {
                continue;
            }
            peak = Math.max(peak, level);
            worst = Math.min(worst, level / peak - 1.0);
        }
        return worst;
    }

    /** Herfindahl index over weights (0-1 scale). Null on empty/degenerate input. */
    public static Double hhi(Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return null;
        }
        double sum = weights.values().stream().filter(w -> w != null && Double.isFinite(w) && w > 0)
                .mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) {
            return null;
        }
        double hhi = weights.values().stream()
                .filter(w -> w != null && Double.isFinite(w) && w > 0)
                .mapToDouble(w -> Math.pow(w / sum, 2)).sum();
        return Double.isFinite(hhi) ? hhi : null;
    }

    /** Descriptive band only — never investment advice. */
    public static String hhiBand(Double hhi) {
        if (hhi == null) {
            return "unknown";
        }
        if (hhi < 0.15) {
            return "diversified";
        }
        if (hhi < 0.25) {
            return "moderately concentrated";
        }
        return "highly concentrated";
    }

    /** Tracking error = annualized std of (portfolio − benchmark) daily returns. */
    public static Double trackingError(List<Double> activeReturns) {
        Double std = sampleStd(activeReturns);
        if (std == null) {
            return null;
        }
        double value = std * Math.sqrt(TRADING_DAYS);
        return Double.isFinite(value) ? value : null;
    }

    private static Double sampleStd(List<Double> values) {
        if (values == null || values.size() < 2) {
            return null;
        }
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (!Double.isFinite(mean)) {
            return null;
        }
        double sumSq = 0.0;
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return null;
            }
            sumSq += (v - mean) * (v - mean);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }

    /** Forward-fill a date→close map across the union calendar; drops leading gaps. */
    public static List<PricePoint> forwardFill(List<LocalDate> calendar, Map<LocalDate, Double> byDate) {
        List<PricePoint> out = new ArrayList<>();
        Double last = null;
        for (LocalDate date : calendar) {
            Double close = byDate.get(date);
            if (close != null && Double.isFinite(close) && close > 0) {
                last = close;
            }
            if (last != null) {
                out.add(new PricePoint(date, last));
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** Sorted union of dates across symbol histories. */
    public static List<LocalDate> unionCalendar(Map<String, Map<LocalDate, Double>> histories) {
        return histories.values().stream()
                .flatMap(m -> m.keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    public static Map<LocalDate, Double> toDateMap(List<PricePoint> points) {
        Map<LocalDate, Double> map = new TreeMap<>();
        for (PricePoint p : points) {
            map.put(p.date(), p.close());
        }
        return map;
    }
}
