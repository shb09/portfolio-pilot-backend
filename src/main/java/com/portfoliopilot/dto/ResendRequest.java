package com.portfoliopilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendRequest(
        @NotBlank(message = "email is required") @Email(message = "must be a valid email") String email) {
}
