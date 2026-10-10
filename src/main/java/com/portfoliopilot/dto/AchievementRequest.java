package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AchievementRequest(
        @NotBlank(message = "title is required") @Size(max = 150) String title,
        String description,
        @Size(max = 7) String date,
        @Size(max = 300) String link) {
}
