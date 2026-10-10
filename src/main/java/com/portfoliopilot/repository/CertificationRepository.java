package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    List<Certification> findByUserIdOrderByIdDesc(Long userId);

    Optional<Certification> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
