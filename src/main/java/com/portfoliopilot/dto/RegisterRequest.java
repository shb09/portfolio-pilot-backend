package com.portfoliopilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registration contract. Email/username are normalized server-side. */
public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[a-z0-9._-]{3,30}$",
                message = "username must be 3-30 chars: lowercase letters, digits, dot, underscore, hyphen")
        String username,
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @NotBlank(message = "email is required") @Email(message = "must be a valid email") @Size(max = 180) String email,
        @NotBlank(message = "password is required") @Size(min = 10, max = 100, message = "password must be 10-100 characters") String password) {
}
