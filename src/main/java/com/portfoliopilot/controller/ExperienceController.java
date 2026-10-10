package com.portfoliopilot.controller;

import com.portfoliopilot.dto.ExperienceDto;
import com.portfoliopilot.dto.ExperienceRequest;
import com.portfoliopilot.service.ExperienceService;
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
@RequestMapping("/api/experience")
public class ExperienceController {

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    public List<ExperienceDto> list(Authentication authentication) {
        return experienceService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperienceDto create(Authentication authentication, @Valid @RequestBody ExperienceRequest request) {
        return experienceService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public ExperienceDto get(Authentication authentication, @PathVariable Long id) {
        return experienceService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public ExperienceDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ExperienceRequest request) {
        return experienceService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        experienceService.delete(authentication.getName(), id);
    }
}
