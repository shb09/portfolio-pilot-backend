package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Education;

public record EducationDto(
        Long id, String school, String degree, String fieldOfStudy,
        Integer startYear, Integer endYear, String description) {

    public static EducationDto from(Education e) {
        return new EducationDto(e.getId(), e.getSchool(), e.getDegree(), e.getFieldOfStudy(),
                e.getStartYear(), e.getEndYear(), e.getDescription());
    }
}
