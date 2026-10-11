package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FinPortfolioRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        String description,
        @Pattern(regexp = "^$|^[A-Z0-9.\\-]{1,20}$",
                message = "benchmarkSymbol must be 1-20 chars: letters, digits, dot, hyphen") @Size(max = 20) String benchmarkSymbol) {
}
