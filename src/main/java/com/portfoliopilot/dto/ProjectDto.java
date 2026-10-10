package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Project;

import java.time.Instant;

public record ProjectDto(
        Long id, String title, String description, String techStack,
        String githubUrl, String liveUrl, String imageUrl, String startDate, String endDate,
        Boolean featured, Instant createdAt, Instant updatedAt) {

    public static ProjectDto from(Project p) {
        return new ProjectDto(p.getId(), p.getTitle(), p.getDescription(), p.getTechStack(),
                p.getGithubUrl(), p.getLiveUrl(), p.getImageUrl(), p.getStartDate(), p.getEndDate(),
                p.getFeatured(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
