package com.example.temporal.transfer.workflow;

import com.example.temporal.common.exception.ValidationException;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Configuration for Temporal activity options.
 * Encapsulates all retry policies and timeouts for different activity types.
 */
final class ActivityConfiguration {

    private static final Duration VALIDATION_TIMEOUT = Duration.ofHours(2);
    private static final Duration ACCOUNT_TIMEOUT = Duration.ofHours(2);
    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration PERSISTENCE_TIMEOUT = Duration.ofSeconds(10);

    private static final RetryOptions VALIDATION_RETRY = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofMinutes(5))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(20)
            .setDoNotRetry(ValidationException.class.getName())
            .build();

    private static final RetryOptions ACCOUNT_RETRY = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofMinutes(2))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(15)
            .build();

    private static final RetryOptions NOTIFICATION_RETRY = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(10)
            .build();

    private static final RetryOptions PERSISTENCE_RETRY = RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setMaximumInterval(Duration.ofSeconds(10))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(15)
            .build();

    private ActivityConfiguration() {
        // Utility class
    }

    static ActivityOptions createValidationOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(VALIDATION_TIMEOUT)
                .setRetryOptions(VALIDATION_RETRY)
                .build();
    }

    static ActivityOptions createAccountOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACCOUNT_TIMEOUT)
                .setRetryOptions(ACCOUNT_RETRY)
                .build();
    }

    static ActivityOptions createNotificationOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(NOTIFICATION_TIMEOUT)
                .setRetryOptions(NOTIFICATION_RETRY)
                .build();
    }

    static ActivityOptions createPersistenceOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(PERSISTENCE_TIMEOUT)
                .setRetryOptions(PERSISTENCE_RETRY)
                .build();
    }
}
