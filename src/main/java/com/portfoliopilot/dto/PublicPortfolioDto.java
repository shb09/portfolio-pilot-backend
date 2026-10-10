package com.portfoliopilot.dto;

import java.util.List;

/** Read-only assembled public portfolio. Served only when published. */
public record PublicPortfolioDto(
        String username,
        String tagline,
        String displayName,
        String headline,
        String about,
        String location,
        String githubUrl,
        String linkedinUrl,
        String resumeUrl,
        List<SkillDto> skills,
        List<ProjectDto> projects,
        List<ExperienceDto> experience,
        List<EducationDto> education,
        List<CertificationDto> certifications,
        List<AchievementDto> achievements) {
}
