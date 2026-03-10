package com.warehouse.inventory.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.inventory.entity.BulkOperationJob;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class BulkJobResponse {

    private final UUID          id;
    private final String        status;
    private final int           totalRows;
    private final int           successfulRows;
    private final int           failedRows;
    private final String        submittedByEmail;
    private final LocalDateTime submittedAt;
    private final LocalDateTime completedAt;

    /**
     * Per-row results parsed from the TEXT JSON column.
     * Each entry: { row, sku, type, quantity, status, reason (optional) }
     */
    private final List<Map<String, Object>> rowResults;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public BulkJobResponse(BulkOperationJob job) {
        this.id               = job.getId();
        this.status           = job.getStatus().name();
        this.totalRows        = job.getTotalRows();
        this.successfulRows   = job.getSuccessfulRows();
        this.failedRows       = job.getFailedRows();
        this.submittedByEmail = job.getSubmittedBy() != null
                ? job.getSubmittedBy().getEmail() : null;
        this.submittedAt      = job.getSubmittedAt();
        this.completedAt      = job.getCompletedAt();
        this.rowResults       = parseRowResults(job.getRowResults());
    }

    private static List<Map<String, Object>> parseRowResults(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}