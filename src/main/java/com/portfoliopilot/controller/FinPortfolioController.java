package com.portfoliopilot.controller;

import com.portfoliopilot.dto.FinPortfolioDto;
import com.portfoliopilot.dto.FinPortfolioRequest;
import com.portfoliopilot.dto.HoldingDto;
import com.portfoliopilot.dto.HoldingRequest;
import com.portfoliopilot.service.FinPortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Owner-scoped investment portfolios and their holdings. */
@RestController
@RequestMapping("/api/fin-portfolios")
public class FinPortfolioController {

    private final FinPortfolioService service;

    public FinPortfolioController(FinPortfolioService service) {
        this.service = service;
    }

    @GetMapping
    public List<FinPortfolioDto> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinPortfolioDto create(Authentication authentication, @Valid @RequestBody FinPortfolioRequest request) {
        return service.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public FinPortfolioDto get(Authentication authentication, @PathVariable Long id) {
        return service.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public FinPortfolioDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody FinPortfolioRequest request) {
        return service.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        service.delete(authentication.getName(), id);
    }

    @GetMapping("/{id}/holdings")
    public List<HoldingDto> holdings(Authentication authentication, @PathVariable Long id) {
        return service.listHoldings(authentication.getName(), id);
    }

    @PostMapping("/{id}/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldingDto addHolding(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody HoldingRequest request) {
        return service.addHolding(authentication.getName(), id, request);
    }

    @PutMapping("/{portfolioId}/holdings/{holdingId}")
    public HoldingDto updateHolding(Authentication authentication, @PathVariable Long portfolioId,
            @PathVariable Long holdingId, @Valid @RequestBody HoldingRequest request) {
        return service.updateHolding(authentication.getName(), portfolioId, holdingId, request);
    }

    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHolding(Authentication authentication, @PathVariable Long portfolioId,
            @PathVariable Long holdingId) {
        service.deleteHolding(authentication.getName(), portfolioId, holdingId);
    }
}
