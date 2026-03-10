package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.response.StockReservationResponse;
import com.warehouse.inventory.entity.StockReservation;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockReservationRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {

    private final StockReservationRepository reservationRepository;
    private final ProductRepository          productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StockReservationResponse> getReservations(UUID productId, String status) {

        User currentUser = getCurrentUser();

        // Parse status filter — null means return all statuses
        StockReservation.Status statusFilter = parseStatus(status);

        // Fetch raw list based on productId filter
        List<StockReservation> reservations;

        if (productId != null) {
            // Validate product exists
            if (!productRepository.existsById(productId)) {
                throw new ResourceNotFoundException("Product not found with id: " + productId);
            }

            reservations = (statusFilter != null)
                    ? reservationRepository.findByProductIdAndStatus(productId, statusFilter)
                    : reservationRepository.findByProductId(productId);
        } else {
            reservations = (statusFilter != null)
                    ? reservationRepository.findByStatus(statusFilter)
                    : reservationRepository.findAllByOrderByCreatedAtDesc();
        }

        // PM scope — filter to only their assigned products
        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            UUID pmId = currentUser.getId();
            reservations = reservations.stream()
                    .filter(r -> r.getProduct().getProductManager() != null
                            && r.getProduct().getProductManager().getId().equals(pmId))
                    .toList();
        }

        return reservations.stream()
                .map(StockReservationResponse::new)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StockReservation.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return StockReservation.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status '" + status + "'. Must be ACTIVE, RELEASED, or EXPIRED");
        }
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}