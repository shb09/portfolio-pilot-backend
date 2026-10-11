package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HoldingRequest(
        @NotBlank(message = "symbol is required")
        @Pattern(regexp = "^[A-Za-z0-9.\\-]{1,20}$",
                message = "symbol must be 1-20 chars: letters, digits, dot, hyphen") String symbol,
        @Size(max = 50) String assetCategory,
        @NotNull(message = "quantity is required") Double quantity) {
}
