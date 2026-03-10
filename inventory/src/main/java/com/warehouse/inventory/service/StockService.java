package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.dto.request.StockUpdateRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StockService {

    StockMovementResponse updateStock(StockUpdateRequest request);

    /**
     * GET /stock/history — dynamic filtered + paginated movement history.
     * PM callers are automatically scoped to their own products.
     */
    PagedResponse<StockMovementResponse> getAllHistory(
            UUID productId,
            StockMovement.MovementType movementType,
            UUID performedById,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    );

    StockMovementResponse getProductHistoryById(UUID productId, int page, int size);
}
