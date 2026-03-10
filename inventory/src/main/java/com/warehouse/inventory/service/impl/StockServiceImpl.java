package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.NotificationService;
import com.warehouse.inventory.service.StockService;
import com.warehouse.inventory.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ThresholdService        thresholdService;
    // Fix: injected here rather than ThresholdService so we can call it AFTER
    // the @Transactional boundary, ensuring StockAlert is committed first
    private final NotificationService     notificationService;

    // -------------------------------------------------------------------------
    // POST /stock/update
    // -------------------------------------------------------------------------

    /**
     * Non-transactional wrapper — orchestrates the transactional work then
     * dispatches async notifications after the transaction has committed.
     * This is the fix for the async/transaction boundary problem.
     */
    @Override
    public StockMovementResponse updateStock(StockUpdateRequest request) {
        // Step 1: do all DB work inside a transaction
        StockUpdateResult result = performStockUpdate(request);

        // Step 2: dispatch async notifications AFTER transaction committed
        // At this point the StockAlert row is fully committed and visible
        // to the notification thread pool
        if (result.alert() != null) {
            try {
                notificationService.sendBreachNotifications(result.alert());
            } catch (Exception e) {
                // Notification failure must never fail the stock update response
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

        // PM ownership check
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

        switch (request.getType()) {
            case "ADD" -> {
                stockAfter = stockBefore + request.getQuantity();
                product.setStockQuantity(stockAfter);
            }
            case "REMOVE" -> {
                int available = stockBefore - product.getReservedQuantity();
                if (available < request.getQuantity()) {
                    throw new InsufficientStockException(
                            "Not enough available stock. Available: " + available
                                    + ", Requested: " + request.getQuantity());
                }
                stockAfter = stockBefore - request.getQuantity();
                product.setStockQuantity(stockAfter);
            }
            default -> throw new IllegalArgumentException(
                    "Type '" + request.getType() + "' not supported. Use ADD or REMOVE.");
        }

        productRepository.save(product);

        // Evaluate thresholds — returns created alert (or null if no breach)
        StockAlert alert = thresholdService.evaluateAndAlert(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .performedBy(currentUser)
                .movementType(StockMovement.MovementType.valueOf(request.getType()))
                .quantity(request.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
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
    public List<StockMovementResponse> getAllHistory(LocalDateTime startDate, LocalDateTime endDate) {

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            UUID managerId = currentUser.getId();
            List<StockMovement> movements = (startDate != null && endDate != null)
                    ? stockMovementRepository
                    .findByProductProductManagerIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            managerId, startDate, endDate)
                    : stockMovementRepository
                    .findByProductProductManagerIdOrderByCreatedAtDesc(managerId);
            return movements.stream().map(StockMovementResponse::new).toList();
        }

        List<StockMovement> movements = (startDate != null && endDate != null)
                ? stockMovementRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate)
                : stockMovementRepository
                .findAllByOrderByCreatedAtDesc();

        return movements.stream().map(StockMovementResponse::new).toList();
    }

    // -------------------------------------------------------------------------
    // GET /stock/history/:productId
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getProductHistory(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            boolean isAssigned = product.getProductManager() != null
                    && product.getProductManager().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new ForbiddenException(
                        "You can only view history for products assigned to you");
            }
        }

        return stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(StockMovementResponse::new)
                .toList();
    }

    @Override
    public List<StockMovementResponse> getHistoryByDate(LocalDateTime startDate, LocalDateTime endDate) {
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    /**
     * Internal result carrier — bundles the movement response and
     * the optional StockAlert so the non-transactional wrapper can
     * dispatch notifications after commit.
     */
    private record StockUpdateResult(StockMovementResponse response, StockAlert alert) {}
}