package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findByUserIdOrderByIdDesc(Long userId);

    Optional<Education> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
