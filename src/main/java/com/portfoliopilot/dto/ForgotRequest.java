package com.portfoliopilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Always returns the same message (no account enumeration). */
public record ForgotRequest(
        @NotBlank(message = "email is required") @Email(message = "must be a valid email") String email) {
}
