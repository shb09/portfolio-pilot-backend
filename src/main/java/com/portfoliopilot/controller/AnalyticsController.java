package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AnalyticsEventRequest;
import com.portfoliopilot.dto.AnalyticsSummaryDto;
import com.portfoliopilot.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /** Public: fired by portfolio visitors. 202 = accepted, no body. */
    @PostMapping("/event")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void record(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsService.record(request);
    }

    /** Private: my engagement numbers for the dashboard. */
    @GetMapping("/summary")
    public AnalyticsSummaryDto summary(Authentication authentication) {
        return analyticsService.summary(authentication.getName());
    }
}
