package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.response.StockAlertResponse;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.StockAlertRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<StockAlertResponse> getAllAlerts() {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            // PM sees only alerts for their assigned products
            return stockAlertRepository.findAll()
                    .stream()
                    .filter(alert -> alert.getProduct().getProductManager() != null
                            && alert.getProduct().getProductManager().getId()
                            .equals(currentUser.getId()))
                    .map(StockAlertResponse::new)
                    .toList();
        }

        return stockAlertRepository.findAll()
                .stream()
                .map(StockAlertResponse::new)
                .toList();
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