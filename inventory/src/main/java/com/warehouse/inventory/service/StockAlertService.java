package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.StockAlertResponse;
import com.warehouse.inventory.entity.StockAlert;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StockAlertService {

    /**
     * GET /api/v1/alerts — dynamic filtered + paginated list.
     * PM callers are automatically scoped to their own products.
     */
    PagedResponse<StockAlertResponse> getAllAlerts(
            UUID productId,
            StockAlert.BreachType breachType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    );

    /** GET /api/v1/alerts/:id — PM scoped */
    StockAlertResponse getAlertById(UUID id);
}
