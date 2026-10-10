package com.portfoliopilot.dto;

import jakarta.validation.constraints.Size;

/** PUT /api/profile body. All fields optional — upsert merges them. */
public record ProfileRequest(
        @Size(max = 150) String headline,
        String about,
        @Size(max = 100) String location,
        @Size(max = 300) String githubUrl,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 300) String resumeUrl) {
}
