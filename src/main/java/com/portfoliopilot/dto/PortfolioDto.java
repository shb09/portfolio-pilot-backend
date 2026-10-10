package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Portfolio;

public record PortfolioDto(String username, Boolean published, String tagline) {

    public static PortfolioDto from(Portfolio p) {
        return new PortfolioDto(p.getUsername(), p.getPublished(), p.getTagline());
    }
}
