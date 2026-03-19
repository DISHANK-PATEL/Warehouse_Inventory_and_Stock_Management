package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.MetricsResponse;
import com.warehouse.inventory.service.MetricsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


@Tag(name = "Metrics")
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    public ResponseEntity<ApiResponse<MetricsResponse>> getMetrics(

            // Convenience: pass number of hours to look back (e.g. 1, 24, 72)
            @RequestParam(required = false) Integer hours,

            // Full custom range — takes precedence over ?hours if both are provided
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to
    ) {
        LocalDateTime resolvedTo   = (to   != null) ? to   : LocalDateTime.now();
        LocalDateTime resolvedFrom;

        if (from != null) {
            resolvedFrom = from;
        } else if (hours != null && hours > 0) {
            resolvedFrom = resolvedTo.minusHours(hours);
        } else {
            // Default: last 24 hours
            resolvedFrom = resolvedTo.minusHours(24);
        }

        return ResponseEntity.ok(ApiResponse.success(
                metricsService.getMetrics(resolvedFrom, resolvedTo)
        ));
    }
}