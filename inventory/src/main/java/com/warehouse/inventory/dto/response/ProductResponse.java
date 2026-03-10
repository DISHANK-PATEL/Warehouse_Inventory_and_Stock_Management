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

    private final int reservedQuantity;
    private final int availableQuantity;
    private final Integer minThreshold;
    private final Integer maxThreshold;
    private final String breachStatus;
    private final ManagerInfo productManager;

    private final UserInfo createdBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.stockQuantity = product.getStockQuantity();
        this.reservedQuantity = product.getReservedQuantity();
        this.availableQuantity = product.getStockQuantity() - product.getReservedQuantity();
        this.minThreshold = product.getMinThreshold();
        this.maxThreshold = product.getMaxThreshold();
        this.breachStatus = product.getBreachStatus().name();
        this.productManager = product.getProductManager() != null
                ? new ManagerInfo(product.getProductManager().getId(), product.getProductManager().getEmail())
                : null;
        this.createdBy = new UserInfo(product.getCreatedBy().getId(), product.getCreatedBy().getEmail());
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }

    @Getter
    public static class ManagerInfo {
        private final UUID id;
        private final String email;

        public ManagerInfo(UUID id, String email) {
            this.id = id;
            this.email = email;
        }
    }

    @Getter
    public static class UserInfo {
        private final UUID id;
        private final String email;

        public UserInfo(UUID id, String email) {
            this.id = id;
            this.email = email;
        }
    }
}