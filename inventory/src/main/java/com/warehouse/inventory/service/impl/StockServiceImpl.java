package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.StockReservation;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.repository.StockReservationRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.NotificationService;
import com.warehouse.inventory.service.StockService;
import com.warehouse.inventory.service.ThresholdService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.specification.StockMovementSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    // Default reservation window: 60 minutes
    private static final int DEFAULT_EXPIRES_IN_MINUTES = 60;

    private final ProductRepository          productRepository;
    private final StockMovementRepository    stockMovementRepository;
    private final StockReservationRepository reservationRepository;
    private final ThresholdService           thresholdService;
    private final NotificationService        notificationService;
    private final MeterRegistry              meterRegistry;

    private Counter addSuccessCounter;
    private Counter addFailCounter;
    private Counter removeSuccessCounter;
    private Counter removeFailCounter;
    private Counter reserveSuccessCounter;
    private Counter reserveFailCounter;
    private Counter releaseSuccessCounter;
    private Counter releaseFailCounter;

    private Timer stockUpdateTimer;

    @PostConstruct
    public void initMetrics() {
        addSuccessCounter    = stockCounter("ADD",     "success");
        addFailCounter       = stockCounter("ADD",     "failure");
        removeSuccessCounter = stockCounter("REMOVE",  "success");
        removeFailCounter    = stockCounter("REMOVE",  "failure");
        reserveSuccessCounter = stockCounter("RESERVE", "success");
        reserveFailCounter    = stockCounter("RESERVE", "failure");
        releaseSuccessCounter = stockCounter("RELEASE", "success");
        releaseFailCounter    = stockCounter("RELEASE", "failure");

        stockUpdateTimer = Timer.builder("stock.updates.duration")
                .description("Time taken to process a stock update operation")
                .register(meterRegistry);
    }

    private Counter stockCounter(String type, String status) {
        return Counter.builder("stock.updates.total")
                .description("Total number of stock update operations")
                .tag("type",   type)
                .tag("status", status)
                .register(meterRegistry);
    }

    // -------------------------------------------------------------------------
    // POST /stock/update  (non-transactional wrapper)
    // -------------------------------------------------------------------------

    @Override
    public StockMovementResponse updateStock(StockUpdateRequest request) {

        String type = request.getType() != null ? request.getType().toUpperCase() : "UNKNOWN";

        StockUpdateResult result;

        try {
            // Time the full transactional operation
            result = stockUpdateTimer.recordCallable(() -> performStockUpdate(request));
        } catch (Exception e) {
            incrementFailCounter(type);
            // Re-throw as unchecked so existing exception handlers still work
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }

        incrementSuccessCounter(type);

        // Dispatch async notifications AFTER transaction committed
        if (result.alert() != null) {
            try {
                notificationService.sendBreachNotifications(result.alert());
            } catch (Exception e) {
                logger.error("Notification dispatch error for alert {}: {}",
                        result.alert().getId(), e.getMessage());
            }
        }

        return result.response();
    }

    @Transactional
    protected StockUpdateResult performStockUpdate(StockUpdateRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        User currentUser = getCurrentUser();

        // PM ownership check applies to all operation types
        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            boolean isAssigned = product.getProductManager() != null
                    && product.getProductManager().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new ForbiddenException(
                        "You can only perform stock operations on products assigned to you");
            }
        }

        int stockBefore = product.getStockQuantity();
        int stockAfter;
        StockAlert alert = null;

        switch (request.getType()) {

            // ------------------------------------------------------------------
            case "ADD" -> {
                stockAfter = stockBefore + request.getQuantity();
                product.setStockQuantity(stockAfter);
                productRepository.save(product);
                alert = thresholdService.evaluateAndAlert(product);
            }

            // ------------------------------------------------------------------
            case "REMOVE" -> {
                int available = stockBefore - product.getReservedQuantity();
                if (available < request.getQuantity()) {
                    throw new InsufficientStockException(
                            "Not enough available stock. Available: " + available
                                    + ", Requested: " + request.getQuantity());
                }
                stockAfter = stockBefore - request.getQuantity();
                product.setStockQuantity(stockAfter);
                productRepository.save(product);
                alert = thresholdService.evaluateAndAlert(product);
            }

            // ------------------------------------------------------------------
            case "RESERVE" -> {
                int available = stockBefore - product.getReservedQuantity();
                if (available < request.getQuantity()) {
                    throw new InsufficientStockException(
                            "Not enough available stock to reserve. Available: " + available
                                    + ", Requested: " + request.getQuantity());
                }

                // Increment reservedQuantity — stockQuantity stays the same
                product.setReservedQuantity(product.getReservedQuantity() + request.getQuantity());
                stockAfter = stockBefore; // physical stock unchanged
                productRepository.save(product);

                // Create the reservation record
                int expiresInMinutes = (request.getExpiresIn() != null && request.getExpiresIn() > 0)
                        ? request.getExpiresIn()
                        : DEFAULT_EXPIRES_IN_MINUTES;

                StockReservation reservation = StockReservation.builder()
                        .product(product)
                        .reservedBy(currentUser)
                        .quantity(request.getQuantity())
                        .status(StockReservation.Status.ACTIVE)
                        .expiresAt(LocalDateTime.now().plusMinutes(expiresInMinutes))
                        .build();

                reservationRepository.save(reservation);

                logger.info("Reserved {} units of '{}' for {} — expires in {} min",
                        request.getQuantity(), product.getName(),
                        currentUser.getEmail(), expiresInMinutes);
            }

            // ------------------------------------------------------------------
            case "RELEASE" -> {
                if (request.getReservationId() == null) {
                    throw new IllegalArgumentException(
                            "reservationId is required for RELEASE operations");
                }

                StockReservation reservation = reservationRepository
                        .findById(request.getReservationId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reservation not found with id: " + request.getReservationId()));

                if (reservation.getStatus() != StockReservation.Status.ACTIVE) {
                    throw new IllegalStateException(
                            "Reservation " + reservation.getId()
                                    + " is not ACTIVE (status: " + reservation.getStatus() + ")");
                }

                // PM can only release reservations on their own products
                if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
                    boolean isAssigned = reservation.getProduct().getProductManager() != null
                            && reservation.getProduct().getProductManager().getId()
                            .equals(currentUser.getId());
                    if (!isAssigned) {
                        throw new ForbiddenException(
                                "You can only release reservations on products assigned to you");
                    }
                }

                // Restore the reserved quantity
                int restored = product.getReservedQuantity() - reservation.getQuantity();
                product.setReservedQuantity(Math.max(0, restored));
                stockAfter = stockBefore; // physical stock unchanged
                productRepository.save(product);

                // Mark reservation as released
                reservation.setStatus(StockReservation.Status.RELEASED);
                reservation.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(reservation);

                logger.info("Released reservation {} — {} units of '{}' restored",
                        reservation.getId(), reservation.getQuantity(), product.getName());
            }

            default -> throw new IllegalArgumentException(
                    "Type '" + request.getType() + "' not supported. Use ADD, REMOVE, RESERVE, or RELEASE.");
        }

        // stockAfter is set by all branches
        // For RESERVE/RELEASE, stockAfter == stockBefore (physical stock unchanged)
        final int finalStockAfter = stockAfter;

        StockMovement movement = StockMovement.builder()
                .product(product)
                .performedBy(currentUser)
                .movementType(StockMovement.MovementType.valueOf(request.getType()))
                .quantity(request.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(finalStockAfter)
                .notes(request.getNotes())
                .build();

        StockMovementResponse response =
                new StockMovementResponse(stockMovementRepository.save(movement));

        return new StockUpdateResult(response, alert);
    }

    // -------------------------------------------------------------------------
    // GET /stock/history
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockMovementResponse> getAllHistory(
            UUID productId,
            StockMovement.MovementType movementType,
            UUID performedById,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    ) {
        User currentUser = getCurrentUser();

        UUID scopedManagerId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId()
                : null;

        Specification<StockMovement> spec = StockMovementSpecification.withFilters(
                productId, movementType, performedById, startDate, endDate, scopedManagerId
        );

        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<StockMovementResponse> resultPage = stockMovementRepository
                .findAll(spec, pageable)
                .map(StockMovementResponse::new);

        return new PagedResponse<>(resultPage);
    }

    // -------------------------------------------------------------------------
    // GET /stock/history/:productId
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StockMovementResponse getProductHistoryById(UUID productId, int page, int size) {
        throw new UnsupportedOperationException(
                "Use getAllHistory with productId filter for paginated product history");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void incrementSuccessCounter(String type) {
        switch (type) {
            case "ADD"     -> addSuccessCounter.increment();
            case "REMOVE"  -> removeSuccessCounter.increment();
            case "RESERVE" -> reserveSuccessCounter.increment();
            case "RELEASE" -> releaseSuccessCounter.increment();
            default        -> {}
        }
    }

    private void incrementFailCounter(String type) {
        switch (type) {
            case "ADD"     -> addFailCounter.increment();
            case "REMOVE"  -> removeFailCounter.increment();
            case "RESERVE" -> reserveFailCounter.increment();
            case "RELEASE" -> releaseFailCounter.increment();
            default        -> {}
        }
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private record StockUpdateResult(StockMovementResponse response, StockAlert alert) {}
}