package com.portfoliopilot.controller;

import com.portfoliopilot.dto.DashboardSummaryDto;
import com.portfoliopilot.dto.ReadinessDto;
import com.portfoliopilot.service.AnalyticsService;
import com.portfoliopilot.service.ReadinessService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Powers the dashboard: "How ready is my professional profile?" */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReadinessService readinessService;
    private final AnalyticsService analyticsService;

    public DashboardController(ReadinessService readinessService, AnalyticsService analyticsService) {
        this.readinessService = readinessService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/readiness")
    public ReadinessDto readiness(Authentication authentication) {
        return readinessService.readiness(authentication.getName());
    }

    @GetMapping("/summary")
    public DashboardSummaryDto summary(Authentication authentication) {
        String email = authentication.getName();
        return new DashboardSummaryDto(
                readinessService.readiness(email),
                analyticsService.summary(email));
    }
}
