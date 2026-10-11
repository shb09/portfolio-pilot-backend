package com.portfoliopilot.repository;

import com.portfoliopilot.entity.AssetPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AssetPriceRepository extends JpaRepository<AssetPrice, Long> {

    List<AssetPrice> findByUserIdAndSymbolOrderByPriceDate(Long userId, String symbol);

    boolean existsByUserIdAndSymbolAndPriceDate(Long userId, String symbol, LocalDate priceDate);

    @Query("SELECT DISTINCT p.symbol FROM AssetPrice p WHERE p.user.id = :userId ORDER BY p.symbol")
    List<String> findDistinctSymbolsByUserId(Long userId);

    long countByUserId(Long userId);
}
