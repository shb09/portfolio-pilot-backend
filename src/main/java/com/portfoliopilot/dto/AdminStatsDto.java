package com.portfoliopilot.dto;

import java.util.List;

/** Real counts only — every number traces to a repository query. */
public record AdminStatsDto(
        long totalUsers,
        long verifiedUsers,
        long pendingVerification,
        long totalPortfolios,
        long publishedPortfolios,
        long totalProjects,
        long totalEvents,
        List<AdminUserDto> recentUsers) {
}
