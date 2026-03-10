package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockAlertRepository;
import com.warehouse.inventory.service.NotificationService;
import com.warehouse.inventory.service.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ThresholdServiceImpl implements ThresholdService {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdServiceImpl.class);

    private final ProductRepository    productRepository;
    private final StockAlertRepository stockAlertRepository;
    private final NotificationService  notificationService;

    // How many alerts of the same breach type can be raised per product
    // within the lookback window before suppression kicks in.
    // Default 2 matches application.properties: notification.breach-limit=2
    @Value("${notification.breach-limit:2}")
    private int breachLimit;

    // Lookback window for breach-limit guard (24 hours)
    private static final int BREACH_LOOKBACK_HOURS = 24;

    // -------------------------------------------------------------------------
    // Core evaluation — called after every stock change
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void evaluateAndAlert(Product product) {
        int stock = product.getStockQuantity();

        Product.BreachStatus newStatus = computeBreachStatus(product, stock);
        Product.BreachStatus oldStatus = product.getBreachStatus();

        // Always update the breach status on the product regardless of alerts
        if (newStatus != oldStatus) {
            product.setBreachStatus(newStatus);
            productRepository.save(product);
            logger.info("Product '{}' breach status: {} → {}", product.getName(), oldStatus, newStatus);
        }

        // Only create an alert if there is an active breach
        if (newStatus == Product.BreachStatus.NONE) {
            return;
        }

        StockAlert.BreachType breachType = newStatus == Product.BreachStatus.BELOW_MIN
                ? StockAlert.BreachType.BELOW_MIN
                : StockAlert.BreachType.ABOVE_MAX;

        int thresholdValue = newStatus == Product.BreachStatus.BELOW_MIN
                ? product.getMinThreshold()
                : product.getMaxThreshold();

        // Breach-limit guard — suppress alert if too many already raised recently
        if (isBreachLimitReached(product, breachType)) {
            logger.info("Breach-limit reached for product '{}' ({}). Suppressing alert.",
                    product.getName(), breachType);
            return;
        }

        // Create the StockAlert
        StockAlert alert = StockAlert.builder()
                .product(product)
                .breachType(breachType)
                .stockAtBreach(stock)
                .thresholdValue(thresholdValue)
                .build();

        alert = stockAlertRepository.save(alert);
        logger.info("StockAlert created for product '{}': {} (stock={}, threshold={})",
                product.getName(), breachType, stock, thresholdValue);

        // Send notifications (non-blocking — failures are logged, not rethrown)
        try {
            notificationService.sendBreachNotifications(alert);
        } catch (Exception e) {
            logger.error("Notification dispatch failed for alert {}: {}", alert.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Determines what the breach status should be given the current stock.
     * BELOW_MIN takes priority over ABOVE_MAX if both somehow apply (shouldn't happen
     * if thresholds are validated, but defensive coding is safer).
     */
    private Product.BreachStatus computeBreachStatus(Product product, int stock) {
        if (product.getMinThreshold() != null && stock < product.getMinThreshold()) {
            return Product.BreachStatus.BELOW_MIN;
        }
        if (product.getMaxThreshold() != null && stock > product.getMaxThreshold()) {
            return Product.BreachStatus.ABOVE_MAX;
        }
        return Product.BreachStatus.NONE;
    }

    /**
     * Returns true if the number of recent alerts for this product + breach type
     * has already reached or exceeded the configured breach-limit.
     * "Recent" is defined as within the last BREACH_LOOKBACK_HOURS hours.
     */
    private boolean isBreachLimitReached(Product product, StockAlert.BreachType breachType) {
        LocalDateTime since = LocalDateTime.now().minusHours(BREACH_LOOKBACK_HOURS);
        long recentCount = stockAlertRepository.countByProductIdAndBreachTypeAndCreatedAtAfter(
                product.getId(), breachType, since
        );
        return recentCount >= breachLimit;
    }
}