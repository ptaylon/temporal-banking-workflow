package com.example.temporal.notification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * JPA entity for notification
 */
@Data
@Entity
@Accessors(chain = true)
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "transfer_id", nullable = false)
    private String transferId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "notification_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus notificationStatus;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    /**
     * Notification status enum
     */
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRYING
    }
}
