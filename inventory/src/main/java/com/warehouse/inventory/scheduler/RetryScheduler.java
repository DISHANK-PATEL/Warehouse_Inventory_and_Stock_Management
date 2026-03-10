package com.warehouse.inventory.scheduler;

import com.warehouse.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that retries failed email notifications.
 *
 * Runs every 5 minutes. Only logs that are:
 *   - status = FAILED
 *   - retryCount < max-retries
 *   - nextRetryAt <= now  (respects exponential backoff)
 * are picked up.
 */
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RetryScheduler.class);

    private final NotificationService notificationService;

    /**
     * fixedDelay=300000 ms = 5 minutes between the end of one run and the start of the next.
     * initialDelay=60000 ms = wait 1 minute after startup before first run.
     */
    @Scheduled(fixedDelayString = "${scheduler.retry-interval-ms:300000}",
            initialDelayString = "${scheduler.retry-initial-delay-ms:60000}")
    public void retryFailedNotifications() {
        logger.debug("RetryScheduler: checking for due notification retries");
        try {
            notificationService.retryFailedNotifications();
        } catch (Exception e) {
            logger.error("RetryScheduler encountered an error: {}", e.getMessage(), e);
        }
    }
}