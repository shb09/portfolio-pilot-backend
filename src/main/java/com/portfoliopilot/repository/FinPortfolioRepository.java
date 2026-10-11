package com.portfoliopilot.repository;

import com.portfoliopilot.entity.FinPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinPortfolioRepository extends JpaRepository<FinPortfolio, Long> {

    List<FinPortfolio> findByUserIdOrderByIdDesc(Long userId);

    Optional<FinPortfolio> findByIdAndUserId(Long id, Long userId);
}
