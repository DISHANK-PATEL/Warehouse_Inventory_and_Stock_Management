package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByProductIdAndStatus(
            UUID productId, StockReservation.Status status
    );

    List<StockReservation> findByProductId(UUID productId);

    List<StockReservation> findByStatus(StockReservation.Status status);

    List<StockReservation> findAllByOrderByCreatedAtDesc();

    List<StockReservation> findByStatusAndExpiresAtBefore(
            StockReservation.Status status, LocalDateTime now
    );

    List<StockReservation> findByProductIdAndReservedByIdAndStatus(
            UUID productId, UUID reservedById, StockReservation.Status status
    );
}
