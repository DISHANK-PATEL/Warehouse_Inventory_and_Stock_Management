package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.StockAlert;

public interface NotificationService {

    /**
     * Sends HTML breach alert emails to the product's assigned PM (if any) and all Admins.
     * Creates a NotificationLog record per recipient.
     *
     * Runs asynchronously on the notificationExecutor thread pool — does NOT block
     * the calling stock update request.
     *
     * IMPORTANT: must only be called AFTER the triggering transaction has committed
     * (i.e. after the StockAlert is visible in the database) to avoid the async
     * thread reading uncommitted data.
     */
    void sendBreachNotifications(StockAlert alert);

    /**
     * Retries FAILED notification logs that are below the max retry count
     * AND whose nextRetryAt time has passed.
     * Called by RetryScheduler on a fixed interval.
     */
    void retryFailedNotifications();
}