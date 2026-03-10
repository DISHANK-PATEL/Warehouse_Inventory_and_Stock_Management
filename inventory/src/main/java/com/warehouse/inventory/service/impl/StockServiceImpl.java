package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.PagedResponse;
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
import com.warehouse.inventory.specification.StockMovementSpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);

    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ThresholdService        thresholdService;
    private final NotificationService     notificationService;

    // -------------------------------------------------------------------------
    // POST /stock/update
    // -------------------------------------------------------------------------

    @Override
    public StockMovementResponse updateStock(StockUpdateRequest request) {
        StockUpdateResult result = performStockUpdate(request);

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
    // GET /stock/history — spec-based, paginated
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

        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return new PagedResponse<>(
                stockMovementRepository.findAll(spec, pageable)
                        .map(StockMovementResponse::new)
        );
    }

    // -------------------------------------------------------------------------
    // GET /stock/history/:productId — paginated, PM scoped
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StockMovementResponse getProductHistoryById(UUID productId, int page, int size) {
        // Delegate to getAllHistory with productId filter
        throw new UnsupportedOperationException(
                "Use GET /stock/history?productId={id} for filtered history");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private record StockUpdateResult(StockMovementResponse response, StockAlert alert) {}
}
