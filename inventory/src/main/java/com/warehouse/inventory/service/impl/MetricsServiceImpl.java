package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.response.MetricsResponse;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.StockReservation;
import com.warehouse.inventory.repository.StockAlertRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.repository.StockReservationRepository;
import com.warehouse.inventory.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private final StockMovementRepository    stockMovementRepository;
    private final StockAlertRepository       stockAlertRepository;
    private final StockReservationRepository reservationRepository;

    @Override
    @Transactional(readOnly = true)
    public MetricsResponse getMetrics(LocalDateTime from, LocalDateTime to) {

        // ── Stock operation counts ────────────────────────────────────────
        long additions    = stockMovementRepository.countByMovementTypeAndCreatedAtBetween(
                StockMovement.MovementType.ADD,     from, to);
        long removals     = stockMovementRepository.countByMovementTypeAndCreatedAtBetween(
                StockMovement.MovementType.REMOVE,  from, to);
        long reservations = stockMovementRepository.countByMovementTypeAndCreatedAtBetween(
                StockMovement.MovementType.RESERVE, from, to);
        long releases     = stockMovementRepository.countByMovementTypeAndCreatedAtBetween(
                StockMovement.MovementType.RELEASE, from, to);
        long totalOps     = additions + removals + reservations + releases;

        // ── Breach counts ─────────────────────────────────────────────────
        long belowMin = stockAlertRepository.countByBreachTypeAndCreatedAtBetween(
                StockAlert.BreachType.BELOW_MIN, from, to);
        long aboveMax = stockAlertRepository.countByBreachTypeAndCreatedAtBetween(
                StockAlert.BreachType.ABOVE_MAX, from, to);
        long totalBreaches = belowMin + aboveMax;

        // ── Reservation counts ────────────────────────────────────────────
        long activeInWindow = reservationRepository.countByStatusAndCreatedAtBetween(
                StockReservation.Status.ACTIVE, from, to);
        long activeNow = reservationRepository.countByStatus(StockReservation.Status.ACTIVE);

        return MetricsResponse.builder()
                .from(from)
                .to(to)
                .totalStockOperations(totalOps)
                .totalStockAdditions(additions)
                .totalStockRemovals(removals)
                .totalStockReservations(reservations)
                .totalStockReleases(releases)
                .totalThresholdBreaches(totalBreaches)
                .belowMinBreaches(belowMin)
                .aboveMaxBreaches(aboveMax)
                .activeReservationsInWindow(activeInWindow)
                .totalActiveReservationsNow(activeNow)
                .build();
    }
}