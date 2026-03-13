package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.MetricsResponse;

import java.time.LocalDateTime;

public interface MetricsService {

    /**
     * Returns business-level metrics for operations that occurred between [from, to].
     *
     * @param from start of the window (inclusive)
     * @param to   end of the window (inclusive)
     */
    MetricsResponse getMetrics(LocalDateTime from, LocalDateTime to);
}