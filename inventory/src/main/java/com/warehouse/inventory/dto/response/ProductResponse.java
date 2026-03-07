package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.Product;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ProductResponse {

    private final UUID id;
    private final String name;
    private final String description;
    private final String sku;
    private final int stockQuantity;
    private final String createdByEmail;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.stockQuantity = product.getStockQuantity();
        this.createdByEmail = product.getCreatedBy().getEmail();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }
}