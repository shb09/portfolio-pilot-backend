package com.portfoliopilot.controller;

import com.portfoliopilot.dto.PortfolioDto;
import com.portfoliopilot.dto.PortfolioRequest;
import com.portfoliopilot.dto.PublicPortfolioDto;
import com.portfoliopilot.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public PortfolioDto getMine(Authentication authentication) {
        return portfolioService.getMine(authentication.getName());
    }

    @PutMapping
    public PortfolioDto upsert(Authentication authentication, @Valid @RequestBody PortfolioRequest request) {
        return portfolioService.upsert(authentication.getName(), request);
    }

    @GetMapping("/preview")
    public PublicPortfolioDto preview(Authentication authentication) {
        return portfolioService.preview(authentication.getName());
    }
}
