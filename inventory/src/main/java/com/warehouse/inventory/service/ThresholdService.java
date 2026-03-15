package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockAlert;

public interface ThresholdService {

    /**
     * Evaluates the product's new stock quantity against its min/max thresholds.
     * If a breach is detected:
     *   1. Updates product.breachStatus
     *   2. Creates a StockAlert (subject to breach-limit guard)
     *   3. Triggers email notifications via NotificationService
     *
     * Must be called after every stock quantity change (ADD or REMOVE).
     * The product must already be saved before calling this.
     */
    StockAlert evaluateAndAlert(Product product);

    boolean isBreachLimitReached(Product product, StockAlert.BreachType breachType);
}