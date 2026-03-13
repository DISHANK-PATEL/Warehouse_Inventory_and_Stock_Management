package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID>,
        JpaSpecificationExecutor<StockAlert> {

    List<StockAlert> findByProductIdOrderByCreatedAtDesc(UUID productId);

    long countByProductIdAndBreachTypeAndCreatedAtAfter(
            UUID productId,
            StockAlert.BreachType breachType,
            LocalDateTime since
    );

    // ---- Metrics queries ----
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByBreachTypeAndCreatedAtBetween(
            StockAlert.BreachType breachType,
            LocalDateTime from,
            LocalDateTime to
    );
}