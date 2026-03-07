package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    // Get complete stock history
    List<StockMovement> findAllByOrderByCreatedAtDesc();

    // Get stock history for a specific product
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);

    // Filter stock history by date range
    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

}