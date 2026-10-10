package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExperienceRequest(
        @NotBlank(message = "title is required") @Size(max = 150) String title,
        @NotBlank(message = "company is required") @Size(max = 150) String company,
        @Size(max = 100) String location,
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "startPeriod must be YYYY-MM") @Size(max = 7) String startPeriod,
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "endPeriod must be YYYY-MM") @Size(max = 7) String endPeriod,
        String description) {
}
