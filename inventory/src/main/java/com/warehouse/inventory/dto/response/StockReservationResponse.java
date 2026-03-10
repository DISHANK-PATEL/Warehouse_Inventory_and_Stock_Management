package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.StockReservation;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockReservationResponse {

    private final UUID          id;
    private final UUID          productId;
    private final String        productName;
    private final String        productSku;
    private final int           quantity;
    private final String        status;
    private final String        reservedByEmail;
    private final LocalDateTime expiresAt;
    private final LocalDateTime releasedAt;
    private final LocalDateTime createdAt;

    public StockReservationResponse(StockReservation reservation) {
        this.id              = reservation.getId();
        this.productId       = reservation.getProduct().getId();
        this.productName     = reservation.getProduct().getName();
        this.productSku      = reservation.getProduct().getSku();
        this.quantity        = reservation.getQuantity();
        this.status          = reservation.getStatus().name();
        this.reservedByEmail = reservation.getReservedBy().getEmail();
        this.expiresAt       = reservation.getExpiresAt();
        this.releasedAt      = reservation.getReleasedAt();
        this.createdAt       = reservation.getCreatedAt();
    }
}