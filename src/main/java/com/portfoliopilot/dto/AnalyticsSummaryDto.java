package com.portfoliopilot.dto;

public record AnalyticsSummaryDto(
        long portfolioViews,
        long projectClicks,
        long githubClicks,
        long resumeClicks,
        long linkedinClicks,
        long totalEvents) {
}
