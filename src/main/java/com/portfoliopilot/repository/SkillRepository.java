package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByUserIdOrderByIdDesc(Long userId);

    Optional<Skill> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
