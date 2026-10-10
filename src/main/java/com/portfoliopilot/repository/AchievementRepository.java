package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findByUserIdOrderByIdDesc(Long userId);

    Optional<Achievement> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
