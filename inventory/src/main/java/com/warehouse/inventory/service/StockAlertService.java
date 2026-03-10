package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.StockAlertResponse;

import java.util.List;
import java.util.UUID;

public interface StockAlertService {

    /** GET /api/v1/alerts — PM scoped */
    List<StockAlertResponse> getAllAlerts();

    /** GET /api/v1/alerts/:id — PM scoped */
    StockAlertResponse getAlertById(UUID id);
}