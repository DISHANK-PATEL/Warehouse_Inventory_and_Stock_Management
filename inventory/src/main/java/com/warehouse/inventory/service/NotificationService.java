package com.warehouse.inventory.service;

import com.warehouse.inventory.entity.StockAlert;

public interface NotificationService {

    void sendBreachNotifications(StockAlert alert);

    void retryFailedNotifications();

    void retriggerNotificationsForAlert(java.util.UUID alertId);
}