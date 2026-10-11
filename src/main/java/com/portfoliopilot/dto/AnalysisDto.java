package com.portfoliopilot.dto;

import java.util.List;
import java.util.Map;

/**
 * Full quantitative report for one portfolio. Null metric = not computable
 * from available data; the matching reason explains why (never NaN/Inf).
 */
public record AnalysisDto(
        Long portfolioId,
        String portfolioName,
        Valuation valuation,
        ReturnsStats returns,
        Allocation allocation,
        BenchmarkComparison benchmark,
        List<String> warnings) {

    public record Position(String symbol, String assetCategory, Double quantity,
            Double lastPrice, String lastDate, Double value, Double weight) {
    }

    public record Valuation(String asOf, Double totalValue, List<Position> positions,
            List<String> missingPrices) {
    }

    public record ReturnsStats(Integer observations, String startDate, String endDate,
            Double cumulativeReturn, Double annualizedReturn, Double annualizedVolatility,
            Double sharpeRatio, Double maxDrawdown, String reason) {
    }

    public record Allocation(Map<String, Double> weights, Double hhi, String hhiBand) {
    }

    public record BenchmarkComparison(String symbol, Integer alignedObservations,
            Double benchmarkCumulativeReturn, Double benchmarkAnnualizedReturn,
            Double activeReturn, Double trackingError, String reason) {
    }
}
