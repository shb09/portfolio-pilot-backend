package com.portfoliopilot.dto;

import com.portfoliopilot.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Fired by the public portfolio page. username = slug in the URL,
 * so no auth needed — the owner is resolved server-side.
 */
public record AnalyticsEventRequest(
        @NotBlank(message = "username is required") String username,
        @NotNull(message = "eventType is required") EventType eventType,
        Long projectId) {
}
