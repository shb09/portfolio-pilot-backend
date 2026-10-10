package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PUT /api/portfolio body: claim a slug, set tagline, flip published. */
public record PortfolioRequest(
        @NotBlank(message = "username is required") @Pattern(regexp = "^[a-z0-9-]{3,30}$",
                message = "username must be 3-30 chars: lowercase letters, digits, hyphens") String username,
        @NotNull(message = "published is required") Boolean published,
        @Size(max = 150) String tagline) {
}
