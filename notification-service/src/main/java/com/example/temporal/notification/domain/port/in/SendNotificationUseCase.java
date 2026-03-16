package com.example.temporal.notification.domain.port.in;

import com.example.temporal.notification.domain.model.NotificationDomain;

import java.util.List;
import java.util.Optional;

/**
 * Use case for sending notifications
 * Defines what the system can do regarding notification sending
 */
public interface SendNotificationUseCase {

    /**
     * Sends a notification for a transfer event
     * @param command the send notification command
     * @return the result of the send operation
     */
    SendNotificationResult sendNotification(SendNotificationCommand command);

    /**
     * Command object for sending notifications
     */
    record SendNotificationCommand(
            String eventType,
            String transferId,
            String accountNumber,
            String message,
            String recipient,
            String idempotencyKey
    ) {
        public SendNotificationCommand {
            if (eventType == null || eventType.trim().isEmpty()) {
                throw new IllegalArgumentException("Event type cannot be null or empty");
            }
            if (transferId == null || transferId.trim().isEmpty()) {
                throw new IllegalArgumentException("Transfer ID cannot be null or empty");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be null or empty");
            }
        }

        public static SendNotificationCommand of(
                String eventType,
                String transferId,
                String accountNumber,
                String message,
                String recipient,
                String idempotencyKey) {
            return new SendNotificationCommand(
                    eventType,
                    transferId,
                    accountNumber,
                    message,
                    recipient,
                    idempotencyKey
            );
        }
    }

    /**
     * Result object for sending notifications
     */
    record SendNotificationResult(
            boolean success,
            String errorMessage,
            Long notificationId
    ) {
        public static SendNotificationResult success(Long notificationId) {
            return new SendNotificationResult(true, null, notificationId);
        }

        public static SendNotificationResult failure(String errorMessage) {
            return new SendNotificationResult(false, errorMessage, null);
        }
    }
}
