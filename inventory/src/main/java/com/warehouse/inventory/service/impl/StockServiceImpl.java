package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.dto.response.StockMovementResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional
    public StockMovementResponse updateStock(StockUpdateRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        int stockBefore = product.getStockQuantity();
        int stockAfter;

        if (request.getType().equals("ADD")) {
            stockAfter = stockBefore + request.getQuantity();
        } else {

            if (stockBefore < request.getQuantity()) {
                throw new InsufficientStockException(
                        "Not enough stock. Available: " + stockBefore +
                                ", Requested: " + request.getQuantity());
            }

            stockAfter = stockBefore - request.getQuantity();
        }

        product.setStockQuantity(stockAfter);
        productRepository.save(product);

        User currentUser = getCurrentUser();

        StockMovement movement = StockMovement.builder()
                .product(product)
                .performedBy(currentUser)
                .movementType(request.getType().equals("ADD")
                        ? StockMovement.MovementType.ADD
                        : StockMovement.MovementType.REMOVE)
                .quantity(request.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(stockAfter)
                .notes(request.getNotes())
                .build();

        return new StockMovementResponse(stockMovementRepository.save(movement));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getAllHistory() {

        return stockMovementRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(StockMovementResponse::new)
                .toList();
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
    public List<StockMovementResponse> getProductHistory(Integer productId) {

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + productId);
        }

        return stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(StockMovementResponse::new)
                .toList();
    }

    private User getCurrentUser() {

        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userDetails.getUser();
    }
}