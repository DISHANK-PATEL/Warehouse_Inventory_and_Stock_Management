package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.service.StockAlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Stock Alerts")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @PostMapping("/{id}/retrigger")
    public ResponseEntity<ApiResponse<String>> retriggerNotifications(
            @PathVariable UUID id) {
        stockAlertService.retriggerNotifications(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Retrigger request processed for alert: " + id));
    }
}
