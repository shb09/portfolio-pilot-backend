package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "title is required") @Size(max = 150) String title,
        @NotBlank(message = "description is required") String description,
        @Size(max = 500) String techStack,
        @Size(max = 300) @Pattern(regexp = "^$|^https?://.+", message = "githubUrl must be a valid http(s) URL") String githubUrl,
        @Size(max = 300) @Pattern(regexp = "^$|^https?://.+", message = "liveUrl must be a valid http(s) URL") String liveUrl,
        @Size(max = 300) @Pattern(regexp = "^$|^https?://.+", message = "imageUrl must be a valid http(s) URL") String imageUrl,
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "startDate must be YYYY-MM") @Size(max = 7) String startDate,
        @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "endDate must be YYYY-MM") @Size(max = 7) String endDate,
        Boolean featured) {
}
