package com.portfoliopilot.dto;

/** Dashboard top-level: readiness + engagement side by side. */
public record DashboardSummaryDto(
        ReadinessDto readiness,
        AnalyticsSummaryDto analytics) {
}
