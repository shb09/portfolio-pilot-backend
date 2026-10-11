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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Analysis windows: full history, date ranges, and the value series. */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private FinPortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private AssetPriceRepository priceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalysisService service;

    private FinPortfolio portfolio;

    private static AssetPrice price(String symbol, String date, double close) {
        AssetPrice p = new AssetPrice();
        p.setSymbol(symbol);
        p.setPriceDate(LocalDate.parse(date));
        p.setClose(close);
        return p;
    }

    @BeforeEach
    void setUp() throws Exception {
        User alice = new User();
        var uid = User.class.getDeclaredField("id");
        uid.setAccessible(true);
        uid.set(alice, 1L);
        alice.setEmail("alice@test.com");
        org.mockito.Mockito.lenient()
                .when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));

        portfolio = new FinPortfolio();
        var pid = FinPortfolio.class.getDeclaredField("id");
        pid.setAccessible(true);
        pid.set(portfolio, 10L);
        portfolio.setUser(alice);
        portfolio.setName("T");
        portfolio.setBenchmarkSymbol("NIFTY50");
        org.mockito.Mockito.lenient()
                .when(portfolioRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(portfolio));

        Holding h = new Holding();
        h.setSymbol("ALPHA");
        h.setQuantity(10.0);
        org.mockito.Mockito.lenient()
                .when(holdingRepository.findByPortfolioIdOrderBySymbol(10L)).thenReturn(List.of(h));

        org.mockito.Mockito.lenient()
                .when(priceRepository.findByUserIdAndSymbolOrderByPriceDate(1L, "ALPHA")).thenReturn(List.of(
                price("ALPHA", "2024-01-31", 100.0),
                price("ALPHA", "2024-02-29", 110.0),
                price("ALPHA", "2024-03-31", 121.0)));
        org.mockito.Mockito.lenient()
                .when(priceRepository.findByUserIdAndSymbolOrderByPriceDate(1L, "NIFTY50")).thenReturn(List.of(
                price("NIFTY50", "2024-01-31", 1000.0),
                price("NIFTY50", "2024-02-29", 1050.0),
                price("NIFTY50", "2024-03-31", 1100.0)));
    }

    @Test
    void fullWindowReportsWholeCurveAndSeries() {
        AnalysisDto report = service.analyze("alice@test.com", 10L);

        assertEquals(2, report.returns().observations());
        assertEquals("2024-01-31", report.returns().startDate());
        assertEquals("2024-03-31", report.returns().endDate());
        assertEquals(3, report.series().size());
        assertEquals("2024-01-31", report.series().get(0).date());
        assertEquals(1000.0, report.series().get(0).value());
        assertEquals(1000.0, report.series().get(0).benchmarkValue());
        assertEquals(2, report.benchmark().alignedObservations());
    }

    @Test
    void dateRangeRestrictsCurveAndBenchmark() {
        AnalysisDto report = service.analyze("alice@test.com", 10L,
                LocalDate.parse("2024-02-29"), LocalDate.parse("2024-03-31"));

        assertEquals(1, report.returns().observations());
        assertEquals("2024-02-29", report.returns().startDate());
        assertEquals("2024-03-31", report.returns().endDate());
        assertEquals(2, report.series().size());
        // Valuation stays point-in-time at latest prices.
        assertEquals(1210.0, report.valuation().totalValue());
        assertEquals(1, report.benchmark().alignedObservations());
    }

    @Test
    void startAfterEndIs400() {
        var ex = assertThrows(ResponseStatusException.class, () -> service.analyze("alice@test.com", 10L,
                LocalDate.parse("2024-03-31"), LocalDate.parse("2024-01-31")));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void emptyRangeExplainsItself() {
        AnalysisDto report = service.analyze("alice@test.com", 10L,
                LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-01"));

        assertTrue(report.series().isEmpty());
        assertNotNull(report.returns().reason());
        assertTrue(report.warnings().stream().anyMatch(w -> w.contains("No valued dates between")));
    }

    @Test
    void seriesCarriesNullBenchmarkWithoutOne() {
        portfolio.setBenchmarkSymbol(null);

        AnalysisDto report = service.analyze("alice@test.com", 10L);

        assertNull(report.benchmark().symbol());
        assertTrue(report.series().stream().allMatch(s -> s.benchmarkValue() == null));
    }
}
