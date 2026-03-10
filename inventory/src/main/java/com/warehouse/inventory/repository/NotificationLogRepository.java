package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByStatusAndRetryCountLessThanAndNextRetryAtBefore(
            NotificationLog.Status status,
            int maxRetries,
            LocalDateTime now
    );

    List<NotificationLog> findByAlertId(UUID alertId);
}
