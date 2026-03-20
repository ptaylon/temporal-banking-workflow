package com.example.temporal.notification.domain.service;

import com.example.temporal.notification.domain.model.NotificationDomain;
import com.example.temporal.notification.domain.port.in.SendNotificationUseCase;
import com.example.temporal.notification.domain.port.in.QueryNotificationUseCase;
import com.example.temporal.notification.domain.port.out.NotificationPersistencePort;
import com.example.temporal.notification.domain.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service implementing notification use cases
 * Contains pure business logic without framework dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements SendNotificationUseCase, QueryNotificationUseCase {

    private final NotificationPersistencePort notificationPersistencePort;
    private final EmailPort emailPort;

    @Override
    @Transactional
    public SendNotificationResult sendNotification(SendNotificationCommand command) {
        log.info("Sending notification for event: {}, transfer: {}", 
                command.eventType(), command.transferId());

        try {
            // Check for duplicate notification (idempotency)
            String idempotencyKey = command.idempotencyKey() != null
                    ? command.idempotencyKey()
                    : UUID.randomUUID().toString();

            var existingNotification = notificationPersistencePort.findByIdempotencyKey(idempotencyKey);
            if (existingNotification.isPresent()) {
                log.info("Notification already exists for idempotency key: {}", idempotencyKey);
                return SendNotificationResult.success(existingNotification.get().getId());
            }

            // Create notification domain object
            NotificationDomain notification = NotificationDomain.create(
                    command.eventType(),
                    command.transferId(),
                    command.accountNumber(),
                    command.message(),
                    command.recipient(),
                    idempotencyKey
            );

            // Save notification as pending
            NotificationDomain savedNotification = notificationPersistencePort.save(notification);

            // Send email if recipient is provided
            if (command.recipient() != null && !command.recipient().trim().isEmpty()) {
                String subject = buildSubject(command.eventType());
                boolean emailSent = emailPort.sendEmail(
                        command.recipient(),
                        subject,
                        command.message()
                );

                if (emailSent) {
                    savedNotification = notificationPersistencePort.save(
                            savedNotification.markAsSent()
                    );
                    log.info("Notification sent successfully for transfer: {}", command.transferId());
                } else {
                    savedNotification = notificationPersistencePort.save(
                            savedNotification.markAsFailed()
                    );
                    log.warn("Failed to send notification for transfer: {}", command.transferId());
                    return SendNotificationResult.failure("Failed to send email");
                }
            } else {
                // No recipient - mark as sent (logging only mode)
                savedNotification = notificationPersistencePort.save(
                        savedNotification.markAsSent()
                );
                log.info("Notification logged (no recipient) for transfer: {}", command.transferId());
            }

            return SendNotificationResult.success(savedNotification.getId());

        } catch (IllegalArgumentException e) {
            log.error("Validation error sending notification: {}", e.getMessage());
            return SendNotificationResult.failure("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage(), e);
            return SendNotificationResult.failure("Failed to send notification: " + e.getMessage());
        }
    }

    @Override
    public Optional<NotificationDomain> getNotificationById(final Long notificationId) {
        log.debug("Getting notification by ID: {}", notificationId);
        return notificationPersistencePort.findById(notificationId);
    }

    @Override
    public List<NotificationDomain> getNotificationsByTransferId(final String transferId) {
        log.debug("Getting notifications by transfer ID: {}", transferId);
        return notificationPersistencePort.findByTransferId(transferId);
    }

    @Override
    public List<NotificationDomain> getNotificationsByAccount(final String accountNumber) {
        log.debug("Getting notifications by account: {}", accountNumber);
        return notificationPersistencePort.findByAccountNumber(accountNumber);
    }

    @Override
    public List<NotificationDomain> getNotificationsByEventType(final String eventType) {
        log.debug("Getting notifications by event type: {}", eventType);
        return notificationPersistencePort.findByEventType(eventType);
    }

    @Override
    public List<NotificationDomain> getNotificationsByStatus(
            final NotificationDomain.NotificationStatus status) {
        log.debug("Getting notifications by status: {}", status);
        return notificationPersistencePort.findByStatus(status);
    }

    @Override
    public List<NotificationDomain> getNotificationsByDateRange(
            final java.time.LocalDateTime start,
            final java.time.LocalDateTime end) {
        // This would require a custom repository method
        // For now, return empty list - can be implemented later
        log.debug("Getting notifications by date range: {} to {}", start, end);
        return List.of();
    }

    /**
     * Builds email subject based on event type
     */
    private String buildSubject(String eventType) {
        return switch (eventType) {
            case "TRANSFER_INITIATED" -> "Transfer Initiated";
            case "TRANSFER_COMPLETED" -> "Transfer Completed Successfully";
            case "TRANSFER_FAILED" -> "Transfer Failed";
            case "TRANSFER_PAUSED" -> "Transfer Paused";
            case "TRANSFER_CANCELLED" -> "Transfer Cancelled";
            default -> "Transfer Notification";
        };
    }
}
