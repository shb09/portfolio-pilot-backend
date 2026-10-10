package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(
        @NotBlank(message = "code is required") String code) {
}
