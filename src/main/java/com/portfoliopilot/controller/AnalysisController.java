package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AnalysisDto;
import com.portfoliopilot.service.AnalysisService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Quantitative report for one owned portfolio. */
@RestController
@RequestMapping("/api/fin-portfolios")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/{id}/analysis")
    public AnalysisDto analyze(Authentication authentication, @PathVariable Long id) {
        return service.analyze(authentication.getName(), id);
    }
}
