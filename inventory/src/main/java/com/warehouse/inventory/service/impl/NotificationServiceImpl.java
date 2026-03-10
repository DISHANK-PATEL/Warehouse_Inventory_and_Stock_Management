package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.entity.NotificationLog;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.NotificationLogRepository;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender             mailSender;
    private final NotificationLogRepository  notificationLogRepository;
    private final UserRepository             userRepository;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    // -------------------------------------------------------------------------
    // Send breach notifications to PM + all Admins
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void sendBreachNotifications(StockAlert alert) {
        List<User> recipients = resolveRecipients(alert);
        for (User recipient : recipients) {
            NotificationLog entry = NotificationLog.builder()
                    .alert(alert)
                    .receiver(recipient)
                    .status(NotificationLog.Status.PENDING)
                    .build();
            entry = notificationLogRepository.save(entry);
            attemptSend(entry, alert, recipient);
        }
    }

    // -------------------------------------------------------------------------
    // Retry failed notifications (called by scheduler in later checkpoint)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void retryFailedNotifications() {
        List<NotificationLog> failedEntries = notificationLogRepository
                .findByStatusAndRetryCountLessThan(NotificationLog.Status.FAILED, maxRetries);
        for (NotificationLog entry : failedEntries) {
            attemptSend(entry, entry.getAlert(), entry.getReceiver());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves all recipients: the assigned PM (if any) + all Admins.
     * De-duplicates in case the PM is also registered as an Admin.
     */
    private List<User> resolveRecipients(StockAlert alert) {
        List<User> recipients = new ArrayList<>();

        User pm = alert.getProduct().getProductManager();
        if (pm != null) {
            recipients.add(pm);
        }

        List<User> admins = userRepository.findAllByRole(User.Role.ADMIN);
        for (User admin : admins) {
            if (pm == null || !admin.getId().equals(pm.getId())) {
                recipients.add(admin);
            }
        }

        return recipients;
    }

    private void attemptSend(NotificationLog entry, StockAlert alert, User recipient) {
        try {
            mailSender.send(buildEmail(alert, recipient));

            entry.setStatus(NotificationLog.Status.DELIVERED);
            entry.setDeliveredAt(LocalDateTime.now());
            entry.setLastAttemptedAt(LocalDateTime.now());
            entry.setFailureReason(null);

        } catch (MailException e) {
            logger.warn("Breach notification failed for {}: {}", recipient.getEmail(), e.getMessage());

            entry.setStatus(NotificationLog.Status.FAILED);
            entry.setFailureReason(e.getMessage());
            entry.setLastAttemptedAt(LocalDateTime.now());
            entry.setRetryCount(entry.getRetryCount() + 1);

            // Exponential backoff: 2^retryCount minutes
            int backoffMinutes = (int) Math.pow(2, entry.getRetryCount());
            entry.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));
        }

        notificationLogRepository.save(entry);
    }

    private SimpleMailMessage buildEmail(StockAlert alert, User recipient) {
        String productName    = alert.getProduct().getName();
        String breachLabel    = alert.getBreachType().name().replace("_", " ");
        int    stockAtBreach  = alert.getStockAtBreach();
        int    thresholdValue = alert.getThresholdValue();

        String subject = String.format("[Warehouse Alert] Stock %s — %s", breachLabel, productName);

        String body = String.format(
                "Hello %s,%n%n"
                        + "A stock threshold breach has been detected.%n%n"
                        + "Product     : %s%n"
                        + "Breach Type : %s%n"
                        + "Stock Level : %d%n"
                        + "Threshold   : %d%n%n"
                        + "Please review and take action in the Warehouse Inventory system.%n%n"
                        + "This is an automated notification.",
                recipient.getFullName(),
                productName, breachLabel,
                stockAtBreach, thresholdValue
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient.getEmail());
        message.setSubject(subject);
        message.setText(body);
        return message;
    }
}