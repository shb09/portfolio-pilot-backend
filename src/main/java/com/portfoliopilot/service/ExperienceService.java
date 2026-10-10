package com.portfoliopilot.service;

import com.portfoliopilot.dto.ExperienceDto;
import com.portfoliopilot.dto.ExperienceRequest;
import com.portfoliopilot.entity.Experience;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.ExperienceRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;

    public ExperienceService(ExperienceRepository experienceRepository, UserRepository userRepository) {
        this.experienceRepository = experienceRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Experience owned(Long id, Long userId) {
        return experienceRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));
    }

    public List<ExperienceDto> list(String email) {
        return experienceRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(ExperienceDto::from).toList();
    }

    @Transactional
    public ExperienceDto create(String email, ExperienceRequest request) {
        Experience experience = new Experience();
        experience.setUser(owner(email));
        apply(experience, request);
        return ExperienceDto.from(experienceRepository.save(experience));
    }

    public ExperienceDto get(String email, Long id) {
        return ExperienceDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public ExperienceDto update(String email, Long id, ExperienceRequest request) {
        Experience experience = owned(id, owner(email).getId());
        apply(experience, request);
        return ExperienceDto.from(experienceRepository.save(experience));
    }

    @Transactional
    public void delete(String email, Long id) {
        experienceRepository.delete(owned(id, owner(email).getId()));
    }

    private void apply(Experience experience, ExperienceRequest request) {
        experience.setTitle(request.title());
        experience.setCompany(request.company());
        experience.setLocation(request.location());
        experience.setStartPeriod(emptyToNull(request.startPeriod()));
        experience.setEndPeriod(emptyToNull(request.endPeriod()));
        experience.setDescription(request.description());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
