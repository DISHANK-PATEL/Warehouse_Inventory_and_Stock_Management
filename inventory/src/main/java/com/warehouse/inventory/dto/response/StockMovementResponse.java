package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.StockMovement;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockMovementResponse {

    private final UUID id;
    private final UUID productId;
    private final String productName;
    private final String movementType;
    private final int quantity;
    private final int stockBefore;
    private final int stockAfter;
    private final String performedByEmail;
    private final String notes;
    private final LocalDateTime createdAt;

    public StockMovementResponse(StockMovement movement) {
        this.id = movement.getId();
        this.productId = movement.getProduct().getId();
        this.productName = movement.getProduct().getName();
        this.movementType = movement.getMovementType().name();
        this.quantity = movement.getQuantity();
        this.stockBefore = movement.getStockBefore();
        this.stockAfter = movement.getStockAfter();
        this.performedByEmail = movement.getPerformedBy().getEmail();
        this.notes = movement.getNotes();
        this.createdAt = movement.getCreatedAt();
    }
}