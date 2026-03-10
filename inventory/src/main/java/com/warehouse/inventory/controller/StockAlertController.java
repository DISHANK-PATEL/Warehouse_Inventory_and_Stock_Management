package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.StockAlertResponse;
import com.warehouse.inventory.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService stockAlertService;

    // GET /api/v1/alerts  (Admin, Staff, PM — PM scoped in service)
    @GetMapping
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getAllAlerts() {
        return ResponseEntity.ok(ApiResponse.success(stockAlertService.getAllAlerts()));
    }

    // GET /api/v1/alerts/:id  (Admin, Staff, PM — PM scoped in service)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockAlertResponse>> getAlertById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(stockAlertService.getAlertById(id)));
    }
}