package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<StockMovementResponse>> updateStock(
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.updateStock(request)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<StockMovementResponse>>> getHistory(
            @RequestParam(required = false)            UUID                       productId,
            @RequestParam(required = false)            StockMovement.MovementType movementType,
            @RequestParam(required = false)            UUID                       performedById,
            @RequestParam(required = false)            LocalDate                  startDate,
            @RequestParam(required = false)            LocalDate                  endDate,
            @RequestParam(defaultValue = "0")          int                        page,
            @RequestParam(defaultValue = "20")         int                        size
    ) {
        var start = startDate != null ? startDate.atStartOfDay()   : null;
        var end   = endDate   != null ? endDate.atTime(23, 59, 59) : null;

        return ResponseEntity.ok(ApiResponse.success(
                stockService.getAllHistory(productId, movementType, performedById, start, end, page, size)
        ));
    }
}
