package com.portfoliopilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EducationRequest(
        @NotBlank(message = "school is required") @Size(max = 150) String school,
        @NotBlank(message = "degree is required") @Size(max = 150) String degree,
        @Size(max = 150) String fieldOfStudy,
        @Min(1950) @Max(2100) Integer startYear,
        @Min(1950) @Max(2100) Integer endYear,
        String description) {
}
