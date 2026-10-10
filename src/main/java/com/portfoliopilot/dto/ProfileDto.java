package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Profile;

public record ProfileDto(
        Long id, String headline, String about, String location,
        String githubUrl, String linkedinUrl, String resumeUrl) {

    public static ProfileDto from(Profile p) {
        return new ProfileDto(p.getId(), p.getHeadline(), p.getAbout(), p.getLocation(),
                p.getGithubUrl(), p.getLinkedinUrl(), p.getResumeUrl());
    }
}
