package com.portfoliopilot.dto;

import com.portfoliopilot.entity.FinPortfolio;

public record FinPortfolioDto(Long id, String name, String description, String benchmarkSymbol, long holdingCount) {

    public static FinPortfolioDto from(FinPortfolio p, long holdingCount) {
        return new FinPortfolioDto(p.getId(), p.getName(), p.getDescription(),
                p.getBenchmarkSymbol(), holdingCount);
    }
}
