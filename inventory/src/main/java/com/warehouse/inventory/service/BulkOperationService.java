package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.BulkJobResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BulkOperationService {

    /**
     * POST /api/v1/bulk/upload
     * Validates the CSV, creates a PROCESSING job, dispatches async processing.
     * Returns immediately with the job ID.
     */
    BulkJobResponse submitJob(MultipartFile file);

    /**
     * GET /api/v1/bulk/{jobId}
     * Returns job status + per-row results. Admin sees any job; Staff/PM see only their own.
     */
    BulkJobResponse getJob(UUID jobId);

    /**
     * GET /api/v1/bulk
     * Lists all jobs. Admin sees all; Staff/PM see only their own.
     */
    List<BulkJobResponse> getAllJobs();
}