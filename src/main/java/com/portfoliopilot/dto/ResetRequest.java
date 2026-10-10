package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetRequest(
        @NotBlank(message = "token is required") String token,
        @NotBlank(message = "password is required") @Size(min = 10, max = 100, message = "password must be 10-100 characters") String newPassword) {
}
