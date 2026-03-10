package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.BulkJobResponse;
import com.warehouse.inventory.service.BulkOperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkOperationController {

    private final BulkOperationService bulkOperationService;

    /**
     * POST /api/v1/bulk/upload
     *
     * Upload a CSV file with stock operations.
     * Expected CSV format:
     *   sku,type,quantity,notes
     *   TW-001,ADD,50,Restock
     *   TW-002,REMOVE,10,Sold
     *
     * Returns immediately with a jobId — poll GET /bulk/{jobId} for results.
     * Role: ADMIN only
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkJobResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)   // 202 — processing started
                .body(ApiResponse.success(bulkOperationService.submitJob(file)));
    }

    /**
     * GET /api/v1/bulk/{jobId}
     *
     * Poll job status and per-row results.
     * Admin sees any job; Staff/PM see only their own.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<BulkJobResponse>> getJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(bulkOperationService.getJob(jobId)));
    }

    /**
     * GET /api/v1/bulk
     *
     * List all bulk jobs.
     * Admin sees all; Staff/PM see only their own.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BulkJobResponse>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.success(bulkOperationService.getAllJobs()));
    }
}