package com.portfoliopilot.controller;

import com.portfoliopilot.dto.ProjectDto;
import com.portfoliopilot.dto.ProjectRequest;
import com.portfoliopilot.service.ProjectService;
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
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> list(Authentication authentication) {
        return projectService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(Authentication authentication, @Valid @RequestBody ProjectRequest request) {
        return projectService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public ProjectDto get(Authentication authentication, @PathVariable Long id) {
        return projectService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public ProjectDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        return projectService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        projectService.delete(authentication.getName(), id);
    }
}
