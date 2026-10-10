package com.portfoliopilot.dto;

import com.portfoliopilot.entity.SkillLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SkillRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @NotNull(message = "level is required (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)") SkillLevel level) {
}
