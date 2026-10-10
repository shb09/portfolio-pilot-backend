package com.portfoliopilot.controller;

import com.portfoliopilot.dto.EducationDto;
import com.portfoliopilot.dto.EducationRequest;
import com.portfoliopilot.service.EducationService;
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
@RequestMapping("/api/education")
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping
    public List<EducationDto> list(Authentication authentication) {
        return educationService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EducationDto create(Authentication authentication, @Valid @RequestBody EducationRequest request) {
        return educationService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public EducationDto get(Authentication authentication, @PathVariable Long id) {
        return educationService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public EducationDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody EducationRequest request) {
        return educationService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        educationService.delete(authentication.getName(), id);
    }
}
