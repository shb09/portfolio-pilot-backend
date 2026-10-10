package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Certification;

public record CertificationDto(
        Long id, String name, String issuer, String issueDate, String credentialUrl) {

    public static CertificationDto from(Certification c) {
        return new CertificationDto(c.getId(), c.getName(), c.getIssuer(),
                c.getIssueDate(), c.getCredentialUrl());
    }
}
