package com.portfoliopilot.controller;

import com.portfoliopilot.dto.CertificationDto;
import com.portfoliopilot.dto.CertificationRequest;
import com.portfoliopilot.service.CertificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificationService certificationService;

    public CertificationController(CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    @GetMapping
    public List<CertificationDto> list(Authentication authentication) {
        return certificationService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CertificationDto create(Authentication authentication, @Valid @RequestBody CertificationRequest request) {
        return certificationService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public CertificationDto get(Authentication authentication, @PathVariable Long id) {
        return certificationService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public CertificationDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody CertificationRequest request) {
        return certificationService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        certificationService.delete(authentication.getName(), id);
    }
}
