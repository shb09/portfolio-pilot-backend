package com.portfoliopilot.service;

import com.portfoliopilot.dto.EducationDto;
import com.portfoliopilot.dto.EducationRequest;
import com.portfoliopilot.entity.Education;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.EducationRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EducationService {

    private final EducationRepository educationRepository;
    private final UserRepository userRepository;

    public EducationService(EducationRepository educationRepository, UserRepository userRepository) {
        this.educationRepository = educationRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Education owned(Long id, Long userId) {
        return educationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found"));
    }

    public List<EducationDto> list(String email) {
        return educationRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(EducationDto::from).toList();
    }

    @Transactional
    public EducationDto create(String email, EducationRequest request) {
        Education education = new Education();
        education.setUser(owner(email));
        apply(education, request);
        return EducationDto.from(educationRepository.save(education));
    }

    public EducationDto get(String email, Long id) {
        return EducationDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public EducationDto update(String email, Long id, EducationRequest request) {
        Education education = owned(id, owner(email).getId());
        apply(education, request);
        return EducationDto.from(educationRepository.save(education));
    }

    @Transactional
    public void delete(String email, Long id) {
        educationRepository.delete(owned(id, owner(email).getId()));
    }

    private void apply(Education education, EducationRequest request) {
        education.setSchool(request.school());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setStartYear(request.startYear());
        education.setEndYear(request.endYear());
        education.setDescription(request.description());
    }
}
