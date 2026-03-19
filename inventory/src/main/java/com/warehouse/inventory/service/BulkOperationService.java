package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.response.BulkJobResponse;

import java.util.List;
import java.util.UUID;

public interface BulkOperationService {

    BulkJobResponse submitJob(byte[] fileBytes, String filename);

    BulkJobResponse getJob(UUID jobId);

    List<BulkJobResponse> getAllJobs();
}