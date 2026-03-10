package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.StockAlert;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockAlertResponse {

    private final UUID   id;
    private final UUID   productId;
    private final String productName;
    private final String breachType;
    private final int    stockAtBreach;
    private final int    thresholdValue;
    private final LocalDateTime createdAt;

    public StockAlertResponse(StockAlert alert) {
        this.id             = alert.getId();
        this.productId      = alert.getProduct().getId();
        this.productName    = alert.getProduct().getName();
        this.breachType     = alert.getBreachType().name();
        this.stockAtBreach  = alert.getStockAtBreach();
        this.thresholdValue = alert.getThresholdValue();
        this.createdAt      = alert.getCreatedAt();
    }
}