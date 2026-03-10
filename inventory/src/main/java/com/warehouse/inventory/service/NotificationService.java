package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.StockAlert;

public interface NotificationService {

    /**
     * Sends breach alert emails to the product's assigned PM (if any) and all Admins.
     * Creates a NotificationLog record per recipient.
     * Called immediately after a StockAlert is persisted.
     */
    void sendBreachNotifications(StockAlert alert);

    /**
     * Retries FAILED notification logs that are below the max retry threshold.
     * Scheduled by the RetryScheduler.
     */
    void retryFailedNotifications();
}