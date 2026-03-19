package com.warehouse.inventory.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
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
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.BulkOperationService;
import com.warehouse.inventory.service.NotificationService;
import com.warehouse.inventory.service.ThresholdService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BulkOperationServiceImpl implements BulkOperationService {

    private static final Logger logger = LoggerFactory.getLogger(BulkOperationServiceImpl.class);

    private static final Set<String> VALID_TYPES = Set.of("ADD", "REMOVE");

    private final BulkOperationJobRepository jobRepository;
    private final ProductRepository          productRepository;
    private final StockMovementRepository    stockMovementRepository;
    private final ThresholdService           thresholdService;
    private final NotificationService        notificationService;
    private final ObjectMapper               objectMapper;
    private final MeterRegistry              meterRegistry;
    private final UserRepository             userRepository;

    @Autowired @Lazy
    private BulkOperationServiceImpl self;

    private Counter jobCompletedCounter;
    private Counter jobFailedCounter;
    private Counter rowSuccessCounter;
    private Counter rowFailedCounter;

    @PostConstruct
    public void initMetrics() {
        jobCompletedCounter = Counter.builder("bulk.jobs.total")
                .tag("status", "COMPLETED").register(meterRegistry);
        jobFailedCounter = Counter.builder("bulk.jobs.total")
                .tag("status", "FAILED").register(meterRegistry);
        rowSuccessCounter = Counter.builder("bulk.rows.processed")
                .tag("status", "SUCCESS").register(meterRegistry);
        rowFailedCounter = Counter.builder("bulk.rows.processed")
                .tag("status", "FAILED").register(meterRegistry);
    }

    @Override
    public BulkJobResponse submitJob(byte[] fileBytes, String filename) {

        User currentUser = getCurrentUser();

        List<String[]> rows = parseCsvBytes(fileBytes);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data rows");
        }

        BulkOperationJob job = BulkOperationJob.builder()
                .submittedBy(currentUser)
                .status(BulkOperationJob.Status.PROCESSING)
                .totalRows(rows.size())
                .successfulRows(0)
                .failedRows(0)
                .build();

        job = jobRepository.save(job);

        logger.info("Bulk job {} created by {} — {} rows to process",
                job.getId(), currentUser.getEmail(), rows.size());

        processJobAsync(job.getId(), fileBytes, currentUser.getId());

        return new BulkJobResponse(job);
    }

    @Async("bulkExecutor")
    public void processJobAsync(UUID jobId, byte[] fileBytes, UUID submittedById) {

        User submittedBy = userRepository.findById(submittedById)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + submittedById));

        List<String[]> rows = parseCsvBytes(fileBytes);
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount    = 0;

        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            String[] cols = rows.get(i);
            Map<String, Object> rowResult = new LinkedHashMap<>();
            rowResult.put("row", rowNumber);

            try {
                if (cols.length < 3) {
                    throw new IllegalArgumentException(
                            "Row must have at least 3 columns: sku, type, quantity");
                }

                String sku    = cols[0].trim();
                String type   = cols[1].trim().toUpperCase();
                String qtyStr = cols[2].trim();
                String notes  = cols.length > 3 ? cols[3].trim() : null;

                rowResult.put("sku",  sku);
                rowResult.put("type", type);

                if (!VALID_TYPES.contains(type)) {
                    throw new IllegalArgumentException(
                            "Invalid type '" + type + "'. Must be ADD or REMOVE");
                }

                int quantity;
                try {
                    quantity = Integer.parseInt(qtyStr);
                    if (quantity < 1) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid quantity '" + qtyStr + "'. Must be a positive integer");
                }
                rowResult.put("quantity", quantity);

                self.processRowTransactional(sku, type, quantity, notes, submittedBy);

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
                        "Insufficient stock. Available: " + available
                                + ", Requested: " + quantity);
            }
            stockAfter = stockBefore - quantity;
            product.setStockQuantity(stockAfter);
        }

        productRepository.save(product);

        var alert = thresholdService.evaluateAndAlert(product);

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

        if (alert != null) {
            try {
                notificationService.sendBreachNotifications(alert);
            } catch (Exception e) {
                logger.warn("Notification failed for {}: {}", product.getName(), e.getMessage());
            }
        }
    }

    @Transactional
    public void finalizeJob(UUID jobId, int successCount, int failCount,
                            List<Map<String, Object>> results) {

        BulkOperationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bulk job not found: " + jobId));

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

    @Override
    @Transactional(readOnly = true)
    public BulkJobResponse getJob(UUID jobId) {
        BulkOperationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bulk job not found: " + jobId));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.ADMIN) {
            if (job.getSubmittedBy() == null ||
                    !job.getSubmittedBy().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("You can only view your own bulk jobs");
            }
        }

        return new BulkJobResponse(job);
    }

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
    // CSV parsing — auto-detects delimiter (comma, semicolon, tab)
    // -------------------------------------------------------------------------

    private List<String[]> parseCsvBytes(byte[] rawBytes) {
        // Strip UTF-8 BOM if present
        byte[] bytes;
        if (rawBytes.length >= 3
                && (rawBytes[0] & 0xFF) == 0xEF
                && (rawBytes[1] & 0xFF) == 0xBB
                && (rawBytes[2] & 0xFF) == 0xBF) {
            bytes = Arrays.copyOfRange(rawBytes, 3, rawBytes.length);
        } else {
            bytes = rawBytes;
        }

        // Detect delimiter from first line
        String firstLine;
        try {
            firstLine = new String(bytes, StandardCharsets.UTF_8).split("\n")[0];
        } catch (Exception e) {
            firstLine = "";
        }

        char delimiter = ',';
        if (firstLine.contains(";")) {
            delimiter = ';';
        } else if (firstLine.contains("\t")) {
            delimiter = '\t';
        }

        logger.debug("CSV delimiter detected: '{}'", delimiter);

        try {
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(delimiter)
                    .build();

            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
                    .withCSVParser(parser)
                    .build()) {

                String[] headers = reader.readNext();

                if (headers == null) {
                    throw new IllegalArgumentException("CSV file is empty");
                }

                // Clean header — strip all non-letter chars (BOM, quotes, spaces)
                String col0 = headers[0].trim().toLowerCase().replaceAll("[^a-z]", "");
                String col1 = headers.length > 1 ? headers[1].trim().toLowerCase().replaceAll("[^a-z]", "") : "";
                String col2 = headers.length > 2 ? headers[2].trim().toLowerCase().replaceAll("[^a-z]", "") : "";

                if (!col0.equals("sku") || !col1.equals("type") || !col2.equals("quantity")) {
                    throw new IllegalArgumentException(
                            "CSV header must be: sku,type,quantity,notes  " +
                                    "(got: '" + col0 + "','" + col1 + "','" + col2 + "')");
                }

                List<String[]> rows = new ArrayList<>();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    rows.add(line);
                }
                return rows;
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV: " + e.getMessage());
        }
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}