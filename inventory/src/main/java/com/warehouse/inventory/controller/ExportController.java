package com.warehouse.inventory.controller;

import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.service.ExportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Export")
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * GET /api/v1/export/products
     *
     * Downloads a CSV of products. Supports all the same filter params
     * as GET /products. PM callers are automatically scoped to their products.
     *
     * Optional params:
     *   ?search=widget
     *   ?managerId=<uuid>
     *   ?assigned=false
     *   ?breachType=BELOW_MIN
     *   ?sortBy=name&sortDir=asc
     */
    @GetMapping("/products")
    public void exportProducts(
            @RequestParam(required = false)           String  search,
            @RequestParam(required = false)           UUID    managerId,
            @RequestParam(required = false)           Boolean assigned,
            @RequestParam(required = false)           String  breachType,
            @RequestParam(defaultValue = "createdAt") String  sortBy,
            @RequestParam(defaultValue = "desc")      String  sortDir,
            HttpServletResponse response
    ) {
        exportService.exportProducts(
                search, managerId, assigned, breachType, sortBy, sortDir, response);
    }

    /**
     * GET /api/v1/export/movements
     *
     * Downloads a CSV of stock movement history.
     * PM callers are automatically scoped to their products.
     *
     * Optional params:
     *   ?productId=<uuid>
     *   ?movementType=ADD|REMOVE|RESERVE|RELEASE
     *   ?startDate=2026-01-01&endDate=2026-03-31
     */
    @GetMapping("/movements")
    public void exportMovements(
            @RequestParam(required = false) UUID   productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) {
        // Parse movementType string to enum (null if not provided)
        StockMovement.MovementType type = null;
        if (movementType != null && !movementType.isBlank()) {
            try {
                type = StockMovement.MovementType.valueOf(movementType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid movementType '" + movementType
                                + "'. Must be ADD, REMOVE, RESERVE, or RELEASE");
            }
        }

        // Convert LocalDate → LocalDateTime for service layer
        LocalDateTime start = startDate != null ? startDate.atStartOfDay()      : null;
        LocalDateTime end   = endDate   != null ? endDate.atTime(23, 59, 59) : null;

        exportService.exportMovements(productId, type, start, end, response);
    }
}