package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

    List<Experience> findByUserIdOrderByIdDesc(Long userId);

    Optional<Experience> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
