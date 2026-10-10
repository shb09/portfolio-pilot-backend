package com.portfoliopilot.service;

import com.portfoliopilot.dto.ProjectDto;
import com.portfoliopilot.dto.ProjectRequest;
import com.portfoliopilot.entity.Project;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.ProjectRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Template every collection module copies: every query scoped by
 * (id, userId). Someone else's id -> 404, never leaks their data.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Project owned(Long id, Long userId) {
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public List<ProjectDto> list(String email) {
        return projectRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(ProjectDto::from).toList();
    }

    @Transactional
    public ProjectDto create(String email, ProjectRequest request) {
        Project project = new Project();
        project.setUser(owner(email));
        apply(project, request);
        return ProjectDto.from(projectRepository.save(project));
    }

    public ProjectDto get(String email, Long id) {
        return ProjectDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public ProjectDto update(String email, Long id, ProjectRequest request) {
        Project project = owned(id, owner(email).getId());
        apply(project, request);
        return ProjectDto.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(String email, Long id) {
        projectRepository.delete(owned(id, owner(email).getId()));
    }

    private void apply(Project project, ProjectRequest request) {
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setTechStack(request.techStack());
        project.setGithubUrl(request.githubUrl());
        project.setLiveUrl(request.liveUrl());
        project.setImageUrl(request.imageUrl());
        project.setStartDate(emptyToNull(request.startDate()));
        project.setEndDate(emptyToNull(request.endDate()));
        project.setFeatured(request.featured() != null && request.featured());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
