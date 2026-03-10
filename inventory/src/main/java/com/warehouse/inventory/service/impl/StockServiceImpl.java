package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.StockService;
import com.warehouse.inventory.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    private final ThresholdService thresholdService;

    @Override
    @Transactional
    public StockMovementResponse updateStock(StockUpdateRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        User currentUser = getCurrentUser();

        int stockBefore = product.getStockQuantity();
        int stockAfter;

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            boolean isAssigned = product.getProductManager() != null
                    && product.getProductManager().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new ForbiddenException(
                        "You can only perform stock operations on products assigned to you");
            }
        }

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
                    "Type '" + request.getType() + "' is not yet handled in stock update");
        }

        productRepository.save(product);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .performedBy(currentUser)
                .movementType(StockMovement.MovementType.valueOf(request.getType()))
                .quantity(request.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .notes(request.getNotes())
                .build();

        return new StockMovementResponse(stockMovementRepository.save(movement));
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getHistoryByDate(
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        return stockMovementRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate)
                .stream()
                .map(StockMovementResponse::new)
                .toList();
    }

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

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}