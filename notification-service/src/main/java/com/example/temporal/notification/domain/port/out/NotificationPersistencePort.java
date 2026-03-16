package com.example.temporal.notification.domain.port.out;

import com.example.temporal.notification.domain.model.NotificationDomain;

import java.util.List;
import java.util.Optional;

/**
 * Port for notification persistence operations
 * Defines what the domain needs from the infrastructure
 */
public interface NotificationPersistencePort {

    /**
     * Saves a notification
     * @param notification the notification to save
     * @return the saved notification
     */
    NotificationDomain save(NotificationDomain notification);

    /**
     * Finds a notification by ID
     * @param id the notification ID
     * @return the notification if found
     */
    Optional<NotificationDomain> findById(Long id);

    /**
     * Finds a notification by idempotency key
     * @param idempotencyKey the idempotency key
     * @return the notification if found
     */
    Optional<NotificationDomain> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds notifications by transfer ID
     * @param transferId the transfer ID
     * @return list of notifications
     */
    List<NotificationDomain> findByTransferId(String transferId);

    /**
     * Finds notifications by account number
     * @param accountNumber the account number
     * @return list of notifications
     */
    List<NotificationDomain> findByAccountNumber(String accountNumber);

    /**
     * Finds notifications by event type
     * @param eventType the event type
     * @return list of notifications
     */
    List<NotificationDomain> findByEventType(String eventType);

    /**
     * Finds notifications by status
     * @param status the notification status
     * @return list of notifications
     */
    List<NotificationDomain> findByStatus(NotificationDomain.NotificationStatus status);
}
