package com.portfoliopilot.dto;

import java.util.List;

/**
 * Backend-calculated profile readiness. The frontend only displays this —
 * scoring rules live in ReadinessService, never in React.
 */
public record ReadinessDto(
        int score,
        List<SectionScore> breakdown,
        List<String> recommendations) {

    public record SectionScore(
            String section,
            String label,
            int weight,
            int earned,
            boolean done) {
    }
}
