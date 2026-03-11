package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.StockMovement;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ExportService {

    /**
     * GET /api/v1/export/products
     * Streams a CSV of products directly to the HTTP response.
     * Supports same filters as GET /products.
     * PM callers are automatically scoped to their assigned products.
     */
    void exportProducts(
            String search,
            UUID managerId,
            Boolean assigned,
            String breachType,
            String sortBy,
            String sortDir,
            HttpServletResponse response
    );

    /**
     * GET /api/v1/export/movements
     * Streams a CSV of stock movements directly to the HTTP response.
     * Supports productId, movementType, date range filters.
     * PM callers are automatically scoped to their assigned products.
     */
    void exportMovements(
            UUID productId,
            StockMovement.MovementType movementType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            HttpServletResponse response
    );
}