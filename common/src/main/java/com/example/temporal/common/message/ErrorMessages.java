package com.example.temporal.common.message;

/**
 * Internationalized error message constants for the banking application.
 * Centralized message management for better maintainability and consistency.
 */
public final class ErrorMessages {

    // ========== ACCOUNT ERRORS ==========
    public static final String ACCOUNT_NOT_FOUND = "error.account.not-found";
    public static final String ACCOUNT_NUMBER_REQUIRED = "error.account.number-required";
    public static final String OWNER_NAME_REQUIRED = "error.account.owner-name-required";
    public static final String BALANCE_CANNOT_BE_NULL = "error.account.balance-cannot-be-null";
    public static final String BALANCE_CANNOT_BE_NEGATIVE = "error.account.balance-cannot-be-negative";
    public static final String CURRENCY_REQUIRED = "error.account.currency-required";
    public static final String INSUFFICIENT_FUNDS = "error.account.insufficient-funds";
    public static final String ACCOUNT_ALREADY_LOCKED = "error.account.already-locked";
    public static final String ACCOUNT_NOT_LOCKED = "error.account.not-locked";

    // ========== TRANSFER ERRORS ==========
    public static final String TRANSFER_NOT_FOUND = "error.transfer.not-found";
    public static final String SOURCE_ACCOUNT_REQUIRED = "error.transfer.source-account-required";
    public static final String DESTINATION_ACCOUNT_REQUIRED = "error.transfer.destination-account-required";
    public static final String ACCOUNTS_CANNOT_BE_SAME = "error.transfer.accounts-cannot-be-same";
    public static final String AMOUNT_MUST_BE_POSITIVE = "error.transfer.amount-must-be-positive";
    public static final String TRANSFER_ALREADY_COMPLETED = "error.transfer.already-completed";
    public static final String TRANSFER_CANNOT_BE_CANCELLED = "error.transfer.cannot-be-cancelled";
    public static final String TRANSFER_CANNOT_BE_PAUSED = "error.transfer.cannot-be-paused";
    public static final String WORKFLOW_NOT_FOUND = "error.transfer.workflow-not-found";

    // ========== VALIDATION ERRORS ==========
    public static final String VALIDATION_FAILED = "error.validation.failed";
    public static final String FRAUD_DETECTED = "error.validation.fraud-detected";
    public static final String TRANSFER_LIMIT_EXCEEDED = "error.validation.limit-exceeded";
    public static final String SOURCE_ACCOUNT_DOES_NOT_EXIST = "error.validation.source-account-not-exist";
    public static final String DESTINATION_ACCOUNT_DOES_NOT_EXIST = "error.validation.destination-account-not-exist";

    // ========== NOTIFICATION ERRORS ==========
    public static final String NOTIFICATION_NOT_FOUND = "error.notification.not-found";
    public static final String EVENT_TYPE_REQUIRED = "error.notification.event-type-required";
    public static final String MESSAGE_REQUIRED = "error.notification.message-required";
    public static final String RECIPIENT_REQUIRED = "error.notification.recipient-required";
    public static final String FAILED_TO_SEND_NOTIFICATION = "error.notification.failed-to-send";

    // ========== AUDIT ERRORS ==========
    public static final String AUDIT_EVENT_NOT_FOUND = "error.audit.event-not-found";
    public static final String EVENT_TYPE_REQUIRED_AUDIT = "error.audit.event-type-required";
    public static final String ENTITY_TYPE_REQUIRED = "error.audit.entity-type-required";

    // ========== GENERAL ERRORS ==========
    public static final String ID_REQUIRED = "error.general.id-required";
    public static final String ID_NOT_FOUND = "error.general.id-not-found";
    public static final String INVALID_STATE = "error.general.invalid-state";
    public static final String OPERATION_NOT_ALLOWED = "error.general.operation-not-allowed";
    public static final String DUPLICATE_ENTRY = "error.general.duplicate-entry";
    public static final String INTERNAL_SERVER_ERROR = "error.general.internal-server-error";
    public static final String METHOD_ARGUMENT_NOT_VALID = "error.general.method-argument-not-valid";
    public static final String ENTITY_NOT_FOUND = "error.general.entity-not-found";
    public static final String VALIDATION_ERROR = "error.general.validation-error";
    public static final String OPERATION_FAILED = "error.general.operation-failed";

    // ========== FEATURE FLAG ERRORS ==========
    public static final String FEATURE_DISABLED = "error.feature.disabled";
    public static final String FEATURE_NOT_FOUND = "error.feature.not-found";

    // ========== CDC ERRORS ==========
    public static final String CDC_EVENT_PROCESSING_FAILED = "error.cdc.event-processing-failed";
    public static final String CDC_PARSING_FAILED = "error.cdc.parsing-failed";

    // ========== ACTIVITY ERRORS ==========
    public static final String ACTIVITY_EXECUTION_FAILED = "error.activity.execution-failed";
    public static final String WORKFLOW_EXECUTION_FAILED = "error.workflow.execution-failed";

    private ErrorMessages() {
        // Utility class - prevent instantiation
    }
}
