package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.entity.NotificationLog;
import com.warehouse.inventory.entity.StockAlert;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.repository.NotificationLogRepository;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;

    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository            userRepository;
    private final SpringTemplateEngine      templateEngine;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    // -------------------------------------------------------------------------
    // Send breach notifications to PM + all Admins
    // -------------------------------------------------------------------------

    @Async("notificationExecutor")
    @Override
    @Transactional
    public void sendBreachNotifications(StockAlert alert) {
        logger.info("Dispatching breach notifications for alert {} (product: {}",
                alert.getId(), alert.getProduct().getName());
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
        List<NotificationLog> dueForRetry = notificationLogRepository
                .findByStatusAndRetryCountLessThanAndNextRetryAtBefore(
                        NotificationLog.Status.FAILED,
                        maxRetries,
                        LocalDateTime.now()
                );

        if (!dueForRetry.isEmpty()) {
            logger.info("Retrying {} failed notification(s)", dueForRetry.size());
        }

        for (NotificationLog entry : dueForRetry) {
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

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(recipient.getEmail());
            helper.setSubject(buildSubject(alert));
            helper.setText(buildHtmlBody(alert, recipient), true);

            mailSender.send(mimeMessage);

            entry.setStatus(NotificationLog.Status.DELIVERED);
            entry.setDeliveredAt(LocalDateTime.now());
            entry.setLastAttemptedAt(LocalDateTime.now());
            entry.setFailureReason(null);

            logger.info("Notification delivered to {}", recipient.getEmail());

        } catch (MailException | MessagingException e) {
            logger.warn("Breach notification failed for {}: {}", recipient.getEmail(), e.getMessage());

            entry.setStatus(NotificationLog.Status.FAILED);
            entry.setFailureReason(e.getMessage());
            entry.setLastAttemptedAt(LocalDateTime.now());
            entry.setRetryCount(entry.getRetryCount() + 1);

            // Exponential backoff: 2^retryCount minutes
            int backoffMinutes = (int) Math.pow(2, entry.getRetryCount());
            entry.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));

            logger.info("Next retry for {} scheduled in {} minute(s)",
                    recipient.getEmail(), backoffMinutes);
        }

        notificationLogRepository.save(entry);
    }

    private String buildSubject(StockAlert alert) {
        String breachLabel = alert.getBreachType().name().replace("_", " ");
        return String.format("[Warehouse Alert] Stock %s — %s",
                breachLabel, alert.getProduct().getName());
    }

    private String buildHtmlBody(StockAlert alert, User recipient) {
        Context ctx = new Context();
        ctx.setVariable("recipientName",  recipient.getFullName());
        ctx.setVariable("productName",    alert.getProduct().getName());
        ctx.setVariable("breachType",     alert.getBreachType().name());
        ctx.setVariable("stockAtBreach",  alert.getStockAtBreach());
        ctx.setVariable("thresholdValue", alert.getThresholdValue());
        ctx.setVariable("detectedAt",
                alert.getCreatedAt() != null
                        ? alert.getCreatedAt().format(DISPLAY_FORMAT)
                        : LocalDateTime.now().format(DISPLAY_FORMAT));

        return templateEngine.process("email/breach-alert", ctx);
    }

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
}