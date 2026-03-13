package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.StockReservationResponse;
import com.warehouse.inventory.service.StockReservationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Stock Reservations")
@RestController
@RequestMapping("/api/v1/stock/reservations")
@RequiredArgsConstructor
public class StockReservationController {

    private final StockReservationService reservationService;

    /**
     * GET /api/v1/stock/reservations
     *
     * Optional query params:
     *   ?productId=<uuid>          — filter by product
     *   ?status=ACTIVE|RELEASED|EXPIRED  — filter by reservation status
     *
     * Roles: ADMIN, STAFF, PRODUCT_MANAGER (PM sees only their products)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StockReservationResponse>>> getReservations(
            @RequestParam(required = false) UUID   productId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.getReservations(productId, status))
        );
    }
}