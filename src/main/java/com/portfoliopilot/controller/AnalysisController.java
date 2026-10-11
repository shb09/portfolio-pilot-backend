package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AnalysisDto;
import com.portfoliopilot.service.AnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Quantitative report for one owned portfolio, optionally date-windowed. */
@RestController
@RequestMapping("/api/fin-portfolios")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/{id}/analysis")
    public AnalysisDto analyze(Authentication authentication, @PathVariable Long id,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return service.analyze(authentication.getName(), id, parseDate(start, "start"), parseDate(end, "end"));
    }

    private static LocalDate parseDate(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use ISO dates (YYYY-MM-DD) for " + name + ".");
        }
    }
}
