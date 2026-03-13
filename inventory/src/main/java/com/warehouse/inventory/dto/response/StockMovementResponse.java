package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.StockMovement;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockMovementResponse {

    private final UUID          id;
    private final UUID          productId;
    private final String        productName;
    private final String        movementType;
    private final int           quantity;
    private final int           stockBefore;
    private final int           stockAfter;

    private final Integer       reservedQuantity;
    private final Integer       availableQuantity;
    private final boolean       thresholdBreached;
    private final String        breachType;

    private final PerformedBy   performedBy;
    private final String        notes;
    private final LocalDateTime createdAt;

    public StockMovementResponse(StockMovement movement) {
        this.id               = movement.getId();
        this.productId        = movement.getProduct().getId();
        this.productName      = movement.getProduct().getName();
        this.movementType     = movement.getMovementType().name();
        this.quantity         = movement.getQuantity();
        this.stockBefore      = movement.getStockBefore();
        this.stockAfter       = movement.getStockAfter();
        this.performedBy = new PerformedBy(movement.getPerformedBy().getEmail());
        this.notes            = movement.getNotes();
        this.createdAt        = movement.getCreatedAt();
        this.reservedQuantity  = null;
        this.availableQuantity = null;
        this.thresholdBreached = false;
        this.breachType        = null;
    }

    public StockMovementResponse(StockMovement movement, int reserved, int available, StockAlert alert) {
        this.id               = movement.getId();
        this.productId        = movement.getProduct().getId();
        this.productName      = movement.getProduct().getName();
        this.movementType     = movement.getMovementType().name();
        this.quantity         = movement.getQuantity();
        this.stockBefore      = movement.getStockBefore();
        this.stockAfter       = movement.getStockAfter();
        this.performedBy = new PerformedBy(movement.getPerformedBy().getEmail());
        this.notes            = movement.getNotes();
        this.createdAt        = movement.getCreatedAt();

        this.reservedQuantity  = reserved;
        this.availableQuantity = available;
        this.thresholdBreached = alert != null;
        this.breachType        = alert != null ? alert.getBreachType().name() : null;
    }

    @Getter
    public static class PerformedBy {
        private final String email;

        public PerformedBy(String email) {
            this.email = email;
        }
    }
}