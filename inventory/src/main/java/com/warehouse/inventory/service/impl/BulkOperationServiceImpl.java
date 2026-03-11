package com.warehouse.inventory.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.warehouse.inventory.dto.response.BulkJobResponse;
import com.warehouse.inventory.entity.BulkOperationJob;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.BulkOperationJobRepository;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.BulkOperationService;
import com.warehouse.inventory.service.ThresholdService;
import com.warehouse.inventory.service.NotificationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BulkOperationServiceImpl implements BulkOperationService {

    private static final Logger logger = LoggerFactory.getLogger(BulkOperationServiceImpl.class);

    // Expected CSV columns (case-insensitive header)
    private static final Set<String> VALID_TYPES = Set.of("ADD", "REMOVE");

    private final BulkOperationJobRepository jobRepository;
    private final ProductRepository          productRepository;
    private final StockMovementRepository    stockMovementRepository;
    private final ThresholdService           thresholdService;
    private final NotificationService        notificationService;
    private final ObjectMapper               objectMapper;
    private final MeterRegistry              meterRegistry;

    private Counter jobCompletedCounter;
    private Counter jobFailedCounter;
    private Counter rowSuccessCounter;
    private Counter rowFailedCounter;

    @PostConstruct
    public void initMetrics() {
        jobCompletedCounter = Counter.builder("bulk.jobs.total")
                .description("Total number of bulk CSV jobs processed")
                .tag("status", "COMPLETED")
                .register(meterRegistry);

        jobFailedCounter = Counter.builder("bulk.jobs.total")
                .description("Total number of bulk CSV jobs processed")
                .tag("status", "FAILED")
                .register(meterRegistry);

        rowSuccessCounter = Counter.builder("bulk.rows.processed")
                .description("Total number of individual CSV rows processed")
                .tag("status", "SUCCESS")
                .register(meterRegistry);

        rowFailedCounter = Counter.builder("bulk.rows.processed")
                .description("Total number of individual CSV rows processed")
                .tag("status", "FAILED")
                .register(meterRegistry);
    }

    // -------------------------------------------------------------------------
    // POST /bulk/upload
    // -------------------------------------------------------------------------

    @Override
    public BulkJobResponse submitJob(MultipartFile file) {

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are accepted");
        }

        User currentUser = getCurrentUser();

        // Parse CSV eagerly to get totalRows and catch format errors before creating job
        List<String[]> rows = parseCsv(file);
        int totalRows = rows.size(); // excludes header

        if (totalRows == 0) {
            throw new IllegalArgumentException("CSV file has no data rows");
        }

        // Create the job in PROCESSING state
        BulkOperationJob job = BulkOperationJob.builder()
                .submittedBy(currentUser)
                .status(BulkOperationJob.Status.PROCESSING)
                .totalRows(totalRows)
                .successfulRows(0)
                .failedRows(0)
                .build();

        job = jobRepository.save(job);

        logger.info("Bulk job {} created by {} — {} rows to process",
                job.getId(), currentUser.getEmail(), totalRows);

        // Dispatch async processing — pass the parsed rows so we don't re-read the stream
        processJobAsync(job.getId(), rows, currentUser);

        return new BulkJobResponse(job);
    }

    // -------------------------------------------------------------------------
    // Async processing — each row in its own transaction
    // -------------------------------------------------------------------------

    @Async("bulkExecutor")
    public void processJobAsync(UUID jobId, List<String[]> rows, User submittedBy) {

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount    = 0;

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2; // 1-based, accounting for header row
            String[] cols = rows.get(i);
            Map<String, Object> rowResult = new LinkedHashMap<>();
            rowResult.put("row", rowNumber);

            try {
                // Expect: sku, type, quantity, notes (notes optional)
                if (cols.length < 3) {
                    throw new IllegalArgumentException(
                            "Row must have at least 3 columns: sku, type, quantity");
                }

                String sku      = cols[0].trim();
                String type     = cols[1].trim().toUpperCase();
                String qtyStr   = cols[2].trim();
                String notes    = cols.length > 3 ? cols[3].trim() : null;

                rowResult.put("sku",  sku);
                rowResult.put("type", type);

                // Validate type
                if (!VALID_TYPES.contains(type)) {
                    throw new IllegalArgumentException(
                            "Invalid type '" + type + "'. Must be ADD or REMOVE");
                }

                // Validate quantity
                int quantity;
                try {
                    quantity = Integer.parseInt(qtyStr);
                    if (quantity < 1) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid quantity '" + qtyStr + "'. Must be a positive integer");
                }
                rowResult.put("quantity", quantity);

                // Process the row inside its own transaction
                processRowTransactional(sku, type, quantity, notes, submittedBy);

                rowResult.put("status", "SUCCESS");
                successCount++;
                rowSuccessCounter.increment();

            } catch (Exception e) {
                rowResult.put("status", "FAILED");
                rowResult.put("reason", e.getMessage());
                failCount++;
                rowFailedCounter.increment();
                logger.warn("Bulk job {} row {}: FAILED — {}", jobId, rowNumber, e.getMessage());
            }

            results.add(rowResult);
        }

        // Finalize the job
        finalizeJob(jobId, successCount, failCount, results);
    }

    @Transactional
    public void processRowTransactional(
            String sku, String type, int quantity, String notes, User performedBy) {

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No product found with SKU: " + sku));

        int stockBefore = product.getStockQuantity();
        int stockAfter;

        if ("ADD".equals(type)) {
            stockAfter = stockBefore + quantity;
            product.setStockQuantity(stockAfter);

        } else {
            int available = stockBefore - product.getReservedQuantity();
            if (available < quantity) {
                throw new IllegalStateException(
                        "Insufficient available stock. Available: " + available
                                + ", Requested: " + quantity);
            }
            stockAfter = stockBefore - quantity;
            product.setStockQuantity(stockAfter);
        }

        productRepository.save(product);

        // Threshold evaluation — alert returned but notified after commit below
        var alert = thresholdService.evaluateAndAlert(product);

        // Record the movement
        StockMovement movement = StockMovement.builder()
                .product(product)
                .performedBy(performedBy)
                .movementType(StockMovement.MovementType.valueOf(type))
                .quantity(quantity)
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .notes(notes != null ? "[BULK] " + notes : "[BULK]")
                .build();

        stockMovementRepository.save(movement);

        // Notify outside this transaction (best-effort, non-blocking)
        if (alert != null) {
            try {
                notificationService.sendBreachNotifications(alert);
            } catch (Exception e) {
                logger.warn("Bulk row notification failed for product {}: {}",
                        product.getName(), e.getMessage());
            }
        }
    }

    @Transactional
    public void finalizeJob(UUID jobId, int successCount, int failCount,
                            List<Map<String, Object>> results) {
        BulkOperationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bulk job not found: " + jobId));

        // FAILED only if every single row failed; otherwise COMPLETED
        BulkOperationJob.Status finalStatus = (failCount == job.getTotalRows())
                ? BulkOperationJob.Status.FAILED
                : BulkOperationJob.Status.COMPLETED;

        job.setStatus(finalStatus);
        job.setSuccessfulRows(successCount);
        job.setFailedRows(failCount);
        job.setCompletedAt(LocalDateTime.now());

        try {
            job.setRowResults(objectMapper.writeValueAsString(results));
        } catch (Exception e) {
            job.setRowResults("[]");
        }

        jobRepository.save(job);

        if (finalStatus == BulkOperationJob.Status.COMPLETED) {
            jobCompletedCounter.increment();
        } else {
            jobFailedCounter.increment();
        }

        logger.info("Bulk job {} {} — {}/{} rows succeeded",
                jobId, finalStatus, successCount, job.getTotalRows());
    }

    // -------------------------------------------------------------------------
    // GET /bulk/{jobId}
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public BulkJobResponse getJob(UUID jobId) {
        BulkOperationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bulk job not found with id: " + jobId));

        User currentUser = getCurrentUser();

        // Non-admin users can only see their own jobs
        if (currentUser.getRole() != User.Role.ADMIN) {
            if (job.getSubmittedBy() == null ||
                    !job.getSubmittedBy().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You can only view your own bulk jobs");
            }
        }

        return new BulkJobResponse(job);
    }

    // -------------------------------------------------------------------------
    // GET /bulk
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<BulkJobResponse> getAllJobs() {
        User currentUser = getCurrentUser();

        List<BulkOperationJob> jobs = (currentUser.getRole() == User.Role.ADMIN)
                ? jobRepository.findAllByOrderBySubmittedAtDesc()
                : jobRepository.findBySubmittedByIdOrderBySubmittedAtDesc(currentUser.getId());

        return jobs.stream().map(BulkJobResponse::new).toList();
    }

    // -------------------------------------------------------------------------
    // CSV parsing
    // -------------------------------------------------------------------------

    private List<String[]> parseCsv(MultipartFile file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream()))) {

            List<String[]> all = reader.readAll();

            if (all.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            validateHeader(all.get(0));

            // Return data rows only (skip header)
            return all.subList(1, all.size());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage());
        }
    }

    private void validateHeader(String[] header) {
        if (header.length < 3) {
            throw new IllegalArgumentException(
                    "CSV must have at least 3 columns: sku, type, quantity");
        }
        String col0 = header[0].trim().toLowerCase();
        String col1 = header[1].trim().toLowerCase();
        String col2 = header[2].trim().toLowerCase();

        if (!col0.equals("sku") || !col1.equals("type") || !col2.equals("quantity")) {
            throw new IllegalArgumentException(
                    "CSV header must be: sku,type,quantity,notes  " +
                            "(got: " + col0 + "," + col1 + "," + col2 + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}