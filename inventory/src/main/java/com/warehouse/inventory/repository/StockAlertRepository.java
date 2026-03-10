package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    List<StockAlert> findByProductIdOrderByCreatedAtDesc(UUID productId);

    long countByProductIdAndBreachTypeAndCreatedAtAfter(
            UUID productId,
            StockAlert.BreachType breachType,
            LocalDateTime since
    );
}
