package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.StockAlertResponse;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<StockAlertResponse>>> getAllAlerts(
            @RequestParam(required = false)            UUID              productId,
            @RequestParam(required = false)            StockAlert.BreachType breachType,
            @RequestParam(required = false)            LocalDate         startDate,
            @RequestParam(required = false)            LocalDate         endDate,
            @RequestParam(defaultValue = "0")          int               page,
            @RequestParam(defaultValue = "20")         int               size
    ) {
        var start = startDate != null ? startDate.atStartOfDay()    : null;
        var end   = endDate   != null ? endDate.atTime(23, 59, 59)  : null;

        return ResponseEntity.ok(ApiResponse.success(
                stockAlertService.getAllAlerts(productId, breachType, start, end, page, size)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockAlertResponse>> getAlertById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(stockAlertService.getAlertById(id)));
    }

    @PostMapping("/{id}/retrigger")
    public ResponseEntity<ApiResponse<String>> retriggerNotifications(
            @PathVariable UUID id) {
        stockAlertService.retriggerNotifications(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Retrigger request processed for alert: " + id));
    }
}
