package com.warehouse.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for the GET /api/v1/metrics endpoint.
 *
 * All counts are scoped to the [from, to] time window supplied by the caller.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse {

    /** Start of the query window (inclusive). */
    private LocalDateTime from;

    /** End of the query window (inclusive). */
    private LocalDateTime to;

    // ── Stock operation counts ────────────────────────────────────────────

    /** Total stock movements (ADD + REMOVE + RESERVE + RELEASE) in window. */
    private long totalStockOperations;

    /** Average stock operations per minute over the window. */
    private double stockOperationsPerMinute;

    /** ADD movements in window. */
    private long totalStockAdditions;

    /** REMOVE movements in window. */
    private long totalStockRemovals;

    /** RESERVE movements in window. */
    private long totalStockReservations;

    /** RELEASE movements in window. */
    private long totalStockReleases;

    // ── Threshold breach counts ───────────────────────────────────────────

    /** Total threshold breach alerts triggered in window. */
    private long totalThresholdBreaches;

    /** BELOW_MIN breach alerts in window. */
    private long belowMinBreaches;

    /** ABOVE_MAX breach alerts in window. */
    private long aboveMaxBreaches;

    // ── Reservation state ─────────────────────────────────────────────────

    /**
     * Reservations that were created in the window AND are still ACTIVE now.
     * (Not a point-in-time snapshot — reflects current DB state filtered by creation time.)
     */
    private long activeReservationsInWindow;

    /** Total currently ACTIVE reservations across the entire system (live gauge). */
    private long totalActiveReservationsNow;
}