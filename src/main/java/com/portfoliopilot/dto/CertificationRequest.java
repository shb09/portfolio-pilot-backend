package com.portfoliopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificationRequest(
        @NotBlank(message = "name is required") @Size(max = 150) String name,
        @NotBlank(message = "issuer is required") @Size(max = 150) String issuer,
        @Size(max = 7) String issueDate,
        @Size(max = 300) String credentialUrl) {
}
