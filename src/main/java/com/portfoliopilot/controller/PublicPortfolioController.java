package com.portfoliopilot.controller;

import com.portfoliopilot.dto.PublicPortfolioDto;
import com.portfoliopilot.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fully public, read-only. No JWT needed — but only published slugs resolve. */
@RestController
@RequestMapping("/portfolio")
public class PublicPortfolioController {

    private final PortfolioService portfolioService;

    public PublicPortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{username}")
    public PublicPortfolioDto getPublic(@PathVariable String username) {
        return portfolioService.getPublic(username);
    }
}
