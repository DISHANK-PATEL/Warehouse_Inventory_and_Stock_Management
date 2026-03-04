package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.service.impl.StockServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockServiceImpl stockService;

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<StockMovementResponse>> updateStock(
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(stockService.updateStock(request)));
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getProductHistory(
            @PathVariable Integer productId) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getProductHistory(productId)));
    }

    @GetMapping("/stock/history")
    public List<StockMovementResponse> getHistory(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {

        if (startDate != null && endDate != null) {
            return stockService.getHistoryByDate(
                    startDate.atStartOfDay(),
                    endDate.atTime(23,59,59)
            );
        }

        return stockService.getAllHistory();
    }
}