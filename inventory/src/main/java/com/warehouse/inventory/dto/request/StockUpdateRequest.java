package com.warehouse.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class StockUpdateRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "ADD|REMOVE", message = "Type must be ADD or REMOVE")
    private String type;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    private String notes;
}