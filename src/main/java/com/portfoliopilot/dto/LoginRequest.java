package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;

/** Single identifier: username OR email, resolved server-side. */
public record LoginRequest(
        @NotBlank(message = "username or email is required") String identifier,
        @NotBlank(message = "password is required") String password) {
}
