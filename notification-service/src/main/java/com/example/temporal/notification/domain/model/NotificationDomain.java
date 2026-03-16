package com.example.temporal.notification.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Pure domain model for Notification - framework independent
 * Represents the core business concept of a notification
 */
@Value
@Builder
@With
public class NotificationDomain {
    Long id;
    String eventType;
    String transferId;
    String accountNumber;
    String message;
    String recipient;
    NotificationStatus status;
    LocalDateTime sentAt;
    LocalDateTime createdAt;
    String idempotencyKey;

    /**
     * Notification status enum
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRYING
    }

    /**
     * Business validation: ensures notification data is valid
     */
    public void validate() {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (transferId == null || transferId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transfer ID cannot be null or empty");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    /**
     * Business rule: can this notification be sent?
     */
    public boolean canBeSent() {
        return status == NotificationStatus.PENDING || status == NotificationStatus.RETRYING;
    }

    /**
     * Business rule: is this notification in a final state?
     */
    public boolean isInFinalState() {
        return status == NotificationStatus.SENT || status == NotificationStatus.FAILED;
    }

    /**
     * Creates a new pending notification
     */
    public static NotificationDomain create(
            String eventType,
            String transferId,
            String accountNumber,
            String message,
            String recipient,
            String idempotencyKey) {

        NotificationDomain notification = NotificationDomain.builder()
                .eventType(eventType)
                .transferId(transferId)
                .accountNumber(accountNumber)
                .message(message)
                .recipient(recipient)
                .status(NotificationStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        notification.validate();
        return notification;
    }

    /**
     * Marks notification as sent - returns new immutable instance
     */
    public NotificationDomain markAsSent() {
        return this.withStatus(NotificationStatus.SENT)
                   .withSentAt(LocalDateTime.now());
    }

    /**
     * Marks notification as failed with retry - returns new immutable instance
     */
    public NotificationDomain markAsFailed() {
        return this.withStatus(NotificationStatus.FAILED);
    }

    /**
     * Marks notification for retry - returns new immutable instance
     */
    public NotificationDomain markForRetry() {
        return this.withStatus(NotificationStatus.RETRYING);
    }
}
