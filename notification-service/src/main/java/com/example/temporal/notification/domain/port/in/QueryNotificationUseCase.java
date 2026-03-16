package com.example.temporal.notification.domain.port.in;

import com.example.temporal.notification.domain.model.NotificationDomain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Use case for querying notifications
 * Defines what the system can do regarding notification queries
 */
public interface QueryNotificationUseCase {

    /**
     * Gets a notification by ID
     * @param notificationId the notification ID
     * @return the notification if found
     */
    Optional<NotificationDomain> getNotificationById(Long notificationId);

    /**
     * Gets notifications by transfer ID
     * @param transferId the transfer ID
     * @return list of notifications for the transfer
     */
    List<NotificationDomain> getNotificationsByTransferId(String transferId);

    /**
     * Gets notifications by account number
     * @param accountNumber the account number
     * @return list of notifications for the account
     */
    List<NotificationDomain> getNotificationsByAccount(String accountNumber);

    /**
     * Gets notifications by event type
     * @param eventType the event type
     * @return list of notifications with the event type
     */
    List<NotificationDomain> getNotificationsByEventType(String eventType);

    /**
     * Gets notifications by status
     * @param status the notification status
     * @return list of notifications with the status
     */
    List<NotificationDomain> getNotificationsByStatus(NotificationDomain.NotificationStatus status);

    /**
     * Gets notifications by date range
     * @param start start date
     * @param end end date
     * @return list of notifications in the date range
     */
    List<NotificationDomain> getNotificationsByDateRange(LocalDateTime start, LocalDateTime end);
}
