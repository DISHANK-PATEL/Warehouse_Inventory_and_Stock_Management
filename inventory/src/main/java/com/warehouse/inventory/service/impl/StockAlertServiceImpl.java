package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.StockAlertResponse;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.StockAlertRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.StockAlertService;
import com.warehouse.inventory.specification.StockAlertSpecification;
import lombok.RequiredArgsConstructor;
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
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertRepository stockAlertRepository;

    // -------------------------------------------------------------------------
    // GET /api/v1/alerts
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockAlertResponse> getAllAlerts(
            UUID productId,
            StockAlert.BreachType breachType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    ) {
        User currentUser = getCurrentUser();

        // PM scope — automatically restrict to their own products
        UUID scopedManagerId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId()
                : null;

        Specification<StockAlert> spec = StockAlertSpecification.withFilters(
                productId, breachType, startDate, endDate, scopedManagerId
        );

        int clampedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, clampedSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return new PagedResponse<>(
                stockAlertRepository.findAll(spec, pageable)
                        .map(StockAlertResponse::new)
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/alerts/:id
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StockAlertResponse getAlertById(UUID id) {
        StockAlert alert = stockAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock alert not found with id: " + id));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            boolean isAssigned = alert.getProduct().getProductManager() != null
                    && alert.getProduct().getProductManager().getId()
                    .equals(currentUser.getId());
            if (!isAssigned) {
                throw new ForbiddenException(
                        "You can only access alerts for products assigned to you");
            }
        }

        return new StockAlertResponse(alert);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}
