package com.portfoliopilot.repository;

import com.portfoliopilot.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByPortfolioIdOrderBySymbol(Long portfolioId);

    Optional<Holding> findByIdAndPortfolioId(Long id, Long portfolioId);

    boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    long countByPortfolioId(Long portfolioId);
}
