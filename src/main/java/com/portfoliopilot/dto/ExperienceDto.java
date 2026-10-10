package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Experience;

public record ExperienceDto(
        Long id, String title, String company, String location,
        String startPeriod, String endPeriod, String description) {

    public static ExperienceDto from(Experience e) {
        return new ExperienceDto(e.getId(), e.getTitle(), e.getCompany(), e.getLocation(),
                e.getStartPeriod(), e.getEndPeriod(), e.getDescription());
    }
}
