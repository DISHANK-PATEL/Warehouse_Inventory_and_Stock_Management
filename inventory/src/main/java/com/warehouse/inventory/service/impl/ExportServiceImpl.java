package com.warehouse.inventory.service.impl;

import com.opencsv.CSVWriter;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.ExportService;
import com.warehouse.inventory.specification.ProductSpecification;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;

    // -------------------------------------------------------------------------
    // GET /export/products
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public void exportProducts(
            String search,
            UUID managerId,
            Boolean assigned,
            String breachType,
            String sortBy,
            String sortDir,
            HttpServletResponse response
    ) {
        User currentUser = getCurrentUser();

        // PM scope — always restrict to their own products
        UUID scopedManagerId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId()
                : null;

        // Parse breachStatus filter
        Product.BreachStatus breachStatus = parseBreachStatus(breachType);

        // Build spec — same logic as ProductServiceImpl
        Specification<Product> spec = ProductSpecification.withFilters(
                search, managerId, assigned, breachStatus, scopedManagerId
        );

        // Build sort
        Sort sort = buildProductSort(sortBy, sortDir);

        List<Product> products = productRepository.findAll(spec, sort);

        // Set response headers
        String filename = "products_export_" + timestamp() + ".csv";
        setCsvHeaders(response, filename);

        // Write CSV
        try (CSVWriter writer = new CSVWriter(response.getWriter())) {

            // Header row
            writer.writeNext(new String[]{
                    "id", "name", "sku", "description",
                    "stockQuantity", "reservedQuantity", "availableQuantity",
                    "minThreshold", "maxThreshold", "breachStatus",
                    "productManagerEmail", "createdByEmail",
                    "createdAt", "updatedAt"
            });

            // Data rows
            for (Product p : products) {
                writer.writeNext(new String[]{
                        str(p.getId()),
                        p.getName(),
                        p.getSku(),
                        nullSafe(p.getDescription()),
                        str(p.getStockQuantity()),
                        str(p.getReservedQuantity()),
                        str(p.getStockQuantity() - p.getReservedQuantity()),
                        str(p.getMinThreshold()),
                        str(p.getMaxThreshold()),
                        p.getBreachStatus().name(),
                        p.getProductManager() != null ? p.getProductManager().getEmail() : "",
                        p.getCreatedBy() != null ? p.getCreatedBy().getEmail() : "",
                        formatDate(p.getCreatedAt()),
                        formatDate(p.getUpdatedAt())
                });
            }

            logger.info("Exported {} products to CSV for user {}",
                    products.size(), currentUser.getEmail());

        } catch (IOException e) {
            logger.error("Failed to write products CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    // -------------------------------------------------------------------------
    // GET /export/movements
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public void exportMovements(
            UUID productId,
            StockMovement.MovementType movementType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            HttpServletResponse response
    ) {
        User currentUser = getCurrentUser();

        // Fetch movements — apply filters progressively using available repo methods
        List<StockMovement> movements = fetchMovements(
                currentUser, productId, movementType, startDate, endDate);

        // Set response headers
        String filename = "movements_export_" + timestamp() + ".csv";
        setCsvHeaders(response, filename);

        // Write CSV
        try (CSVWriter writer = new CSVWriter(response.getWriter())) {

            // Header row
            writer.writeNext(new String[]{
                    "id", "productId", "productName", "sku",
                    "movementType", "quantity", "stockBefore", "stockAfter",
                    "performedByEmail", "notes", "createdAt"
            });

            // Data rows
            for (StockMovement m : movements) {
                writer.writeNext(new String[]{
                        str(m.getId()),
                        str(m.getProduct().getId()),
                        m.getProduct().getName(),
                        m.getProduct().getSku(),
                        m.getMovementType().name(),
                        str(m.getQuantity()),
                        str(m.getStockBefore()),
                        str(m.getStockAfter()),
                        m.getPerformedBy() != null ? m.getPerformedBy().getEmail() : "",
                        nullSafe(m.getNotes()),
                        formatDate(m.getCreatedAt())
                });
            }

            logger.info("Exported {} movements to CSV for user {}",
                    movements.size(), currentUser.getEmail());

        } catch (IOException e) {
            logger.error("Failed to write movements CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    // -------------------------------------------------------------------------
    // Movement fetching — applies filters using existing repo methods
    // then applies movementType and PM scope in-stream (lightweight)
    // -------------------------------------------------------------------------

    private List<StockMovement> fetchMovements(
            User currentUser,
            UUID productId,
            StockMovement.MovementType movementType,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<StockMovement> movements;

        boolean hasDateRange = startDate != null && endDate != null;
        UUID pmId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId() : null;

        if (productId != null) {
            movements = hasDateRange
                    ? stockMovementRepository
                    .findByProductIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            productId, startDate, endDate)
                    : stockMovementRepository
                    .findByProductIdOrderByCreatedAtDesc(productId);
        } else if (pmId != null) {
            movements = hasDateRange
                    ? stockMovementRepository
                    .findByProductProductManagerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            pmId, startDate, endDate)
                    : stockMovementRepository
                    .findByProductProductManagerIdOrderByCreatedAtDesc(pmId);
        } else {
            movements = hasDateRange
                    ? stockMovementRepository
                    .findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate)
                    : stockMovementRepository.findAllByOrderByCreatedAtDesc();
        }

        // Apply movementType filter in-stream (avoids adding more repo methods)
        if (movementType != null) {
            movements = movements.stream()
                    .filter(m -> m.getMovementType() == movementType)
                    .toList();
        }

        return movements;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setCsvHeaders(HttpServletResponse response, String filename) {
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(FORMATTER) : "";
    }

    private String str(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private Product.BreachStatus parseBreachStatus(String breachType) {
        if (breachType == null || breachType.isBlank()) return null;
        try {
            return Product.BreachStatus.valueOf(breachType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid breachType '" + breachType + "'. Must be NONE, BELOW_MIN, or ABOVE_MAX");
        }
    }

    private Sort buildProductSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "name"          -> "name";
            case "stockquantity" -> "stockQuantity";
            default              -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}