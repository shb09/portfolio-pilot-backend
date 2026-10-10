package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);

    Optional<Portfolio> findByUsername(String username);

    long countByPublished(boolean published);
}
