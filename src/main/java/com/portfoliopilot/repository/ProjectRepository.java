package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByIdDesc(Long userId);

    /** Scoped lookup: one query enforces ownership (404 covers not-found + not-yours). */
    Optional<Project> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
