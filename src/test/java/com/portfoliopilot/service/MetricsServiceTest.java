package com.portfoliopilot.service;

import com.portfoliopilot.service.MetricsService.PricePoint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hand-computed expectations — no Spring context needed. */
class MetricsServiceTest {

    private static List<PricePoint> series(double... closes) {
        LocalDate start = LocalDate.of(2024, 1, 1);
        java.util.List<PricePoint> out = new java.util.ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            out.add(new PricePoint(start.plusDays(i), closes[i]));
        }
        return out;
    }

    @Test
    void dailyReturnsMatchHandCalculation() {
        List<Double> rets = MetricsService.dailyReturns(series(100.0, 110.0, 99.0));
        assertEquals(2, rets.size());
        assertEquals(0.10, rets.get(0), 1e-12);
        assertEquals(99.0 / 110.0 - 1.0, rets.get(1), 1e-12);
    }

    @Test
    void cumulativeAndAnnualizedMatch() {
        var prices = series(100.0, 110.0);
        Double cum = MetricsService.cumulativeReturn(prices);
        assertEquals(0.10, cum, 1e-12);
        assertEquals(Math.pow(1.10, 252.0) - 1.0, MetricsService.annualizedReturn(cum, 1), 1e-9);
    }

    @Test
    void flatSeriesHasZeroVolatilityAndNullSharpe() {
        var prices = series(100.0, 100.0, 100.0);
        List<Double> rets = MetricsService.dailyReturns(prices);
        assertEquals(0.0, MetricsService.annualizedVolatility(rets), 1e-12);
        assertNull(MetricsService.sharpeRatio(rets, 0.0));
    }

    @Test
    void maxDrawdownMatchesHandCalculation() {
        // 100 -> 120 (+20%) -> 90 (-25% from peak) -> 99
        Double dd = MetricsService.maxDrawdown(series(100.0, 120.0, 90.0, 99.0));
        assertEquals(90.0 / 120.0 - 1.0, dd, 1e-12);
    }

    @Test
    void hhiWeightsAndBands() {
        assertEquals(1.0, MetricsService.hhi(Map.of("A", 1.0)), 1e-12);
        assertEquals(0.5, MetricsService.hhi(Map.of("A", 0.5, "B", 0.5)), 1e-12);
        assertEquals("diversified", MetricsService.hhiBand(0.10));
        assertEquals("moderately concentrated", MetricsService.hhiBand(0.20));
        assertEquals("highly concentrated", MetricsService.hhiBand(0.90));
        assertEquals("unknown", MetricsService.hhiBand(null));
    }

    @Test
    void insufficientDataYieldsNullsNotNaN() {
        assertNull(MetricsService.cumulativeReturn(series(100.0)));
        assertNull(MetricsService.annualizedVolatility(List.of(0.01)));
        assertNull(MetricsService.maxDrawdown(series(100.0)));
        assertNull(MetricsService.hhi(Map.of()));
        assertTrue(MetricsService.dailyReturns(series(100.0)).isEmpty());
    }

    @Test
    void forwardFillDropsLeadingGaps() {
        var calendar = List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));
        var filled = MetricsService.forwardFill(calendar, Map.of(LocalDate.of(2024, 1, 2), 50.0));
        assertEquals(2, filled.size());
        assertEquals(50.0, filled.get(0).close(), 1e-12);
        assertEquals(50.0, filled.get(1).close(), 1e-12);
    }
}
