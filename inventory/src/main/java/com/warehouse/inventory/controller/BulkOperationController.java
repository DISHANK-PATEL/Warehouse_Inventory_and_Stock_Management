package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.BulkJobResponse;
import com.warehouse.inventory.service.BulkOperationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Bulk Operations")
@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkOperationController {

    private final BulkOperationService bulkOperationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BulkJobResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        if (file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase().endsWith(".csv"))
            throw new IllegalArgumentException("Only CSV files are supported");

        try {
            byte[] fileBytes = file.getBytes();
            BulkJobResponse response = bulkOperationService.submitJob(fileBytes, file.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read file: " + e.getMessage());
        }
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<BulkJobResponse>> getJob(
            @PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.success(bulkOperationService.getJob(jobId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BulkJobResponse>>> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.success(bulkOperationService.getAllJobs()));
    }
}