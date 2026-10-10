package com.portfoliopilot.service;

import com.portfoliopilot.dto.CertificationDto;
import com.portfoliopilot.dto.CertificationRequest;
import com.portfoliopilot.entity.Certification;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.CertificationRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;

    public CertificationService(CertificationRepository certificationRepository, UserRepository userRepository) {
        this.certificationRepository = certificationRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Certification owned(Long id, Long userId) {
        return certificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found"));
    }

    public List<CertificationDto> list(String email) {
        return certificationRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(CertificationDto::from).toList();
    }

    @Transactional
    public CertificationDto create(String email, CertificationRequest request) {
        Certification certification = new Certification();
        certification.setUser(owner(email));
        apply(certification, request);
        return CertificationDto.from(certificationRepository.save(certification));
    }

    public CertificationDto get(String email, Long id) {
        return CertificationDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public CertificationDto update(String email, Long id, CertificationRequest request) {
        Certification certification = owned(id, owner(email).getId());
        apply(certification, request);
        return CertificationDto.from(certificationRepository.save(certification));
    }

    @Transactional
    public void delete(String email, Long id) {
        certificationRepository.delete(owned(id, owner(email).getId()));
    }

    private void apply(Certification certification, CertificationRequest request) {
        certification.setName(request.name());
        certification.setIssuer(request.issuer());
        certification.setIssueDate(request.issueDate());
        certification.setCredentialUrl(request.credentialUrl());
    }
}
