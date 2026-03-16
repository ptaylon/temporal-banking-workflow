package com.example.temporal.common.message;

/**
 * Internationalized success message constants for the banking application.
 * Centralized message management for better maintainability and consistency.
 */
public final class SuccessMessages {

    // ========== ACCOUNT MESSAGES ==========
    public static final String ACCOUNT_CREATED = "success.account.created";
    public static final String ACCOUNT_UPDATED = "success.account.updated";
    public static final String ACCOUNT_DELETED = "success.account.deleted";
    public static final String ACCOUNT_LOCKED = "success.account.locked";
    public static final String ACCOUNT_UNLOCKED = "success.account.unlocked";
    public static final String ACCOUNT_DEBITED = "success.account.debited";
    public static final String ACCOUNT_CREDITED = "success.account.credited";
    public static final String BALANCE_RETRIEVED = "success.account.balance-retrieved";

    // ========== TRANSFER MESSAGES ==========
    public static final String TRANSFER_INITIATED = "success.transfer.initiated";
    public static final String TRANSFER_COMPLETED = "success.transfer.completed";
    public static final String TRANSFER_CANCELLED = "success.transfer.cancelled";
    public static final String TRANSFER_PAUSED = "success.transfer.paused";
    public static final String TRANSFER_RESUMED = "success.transfer.resumed";
    public static final String TRANSFER_VALIDATED = "success.transfer.validated";
    public static final String TRANSFER_STATUS_RETRIEVED = "success.transfer.status-retrieved";

    // ========== VALIDATION MESSAGES ==========
    public static final String VALIDATION_APPROVED = "success.validation.approved";
    public static final String VALIDATION_COMPLETED = "success.validation.completed";

    // ========== NOTIFICATION MESSAGES ==========
    public static final String NOTIFICATION_SENT = "success.notification.sent";
    public static final String NOTIFICATION_CREATED = "success.notification.created";
    public static final String NOTIFICATION_UPDATED = "success.notification.updated";

    // ========== AUDIT MESSAGES ==========
    public static final String AUDIT_EVENT_RECORDED = "success.audit.event-recorded";
    public static final String AUDIT_EVENTS_RETRIEVED = "success.audit.events-retrieved";

    // ========== GENERAL MESSAGES ==========
    public static final String OPERATION_SUCCESSFUL = "success.general.operation-successful";
    public static final String REQUEST_PROCESSED = "success.general.request-processed";

    private SuccessMessages() {
        // Utility class - prevent instantiation
    }
}
