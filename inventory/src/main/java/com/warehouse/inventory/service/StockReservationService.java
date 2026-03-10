package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.StockReservationResponse;

import java.util.List;
import java.util.UUID;

public interface StockReservationService {

    /**
     * GET /api/v1/stock/reservations
     *
     * @param productId  Optional — filter by product
     * @param status     Optional — ACTIVE | RELEASED | EXPIRED
     * Admin/Staff see all; PM sees only reservations for their assigned products.
     */
    List<StockReservationResponse> getReservations(UUID productId, String status);
}