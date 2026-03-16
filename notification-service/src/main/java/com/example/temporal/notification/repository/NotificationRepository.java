package com.example.temporal.notification.repository;

import com.example.temporal.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for notification entities
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Finds a notification by idempotency key
     */
    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds notifications by transfer ID
     */
    List<NotificationEntity> findByTransferId(String transferId);

    /**
     * Finds notifications by account number
     */
    List<NotificationEntity> findByAccountNumber(String accountNumber);

    /**
     * Finds notifications by event type
     */
    List<NotificationEntity> findByEventType(String eventType);

    /**
     * Finds notifications by status
     */
    List<NotificationEntity> findByNotificationStatus(NotificationEntity.NotificationStatus status);
}
