package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.model.TransferStatus;
import io.temporal.workflow.Workflow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Temporal search attributes for workflow visibility.
 * Skips operations in test environments to avoid errors.
 */
final class SearchAttributesManager {

    private static final String TEST_NAMESPACE = "UnitTest";
    private static final int MAX_AMOUNT_FOR_PRIORITY = 100_000;
    private static final int HIGH_AMOUNT_FOR_PRIORITY = 50_000;
    private static final int MEDIUM_AMOUNT_FOR_PRIORITY = 10_000;
    private static final int STANDARD_AMOUNT_FOR_PRIORITY = 1_000;
    private static final int LOW_AMOUNT_FOR_PRIORITY = 100;

    private final String namespace;

    SearchAttributesManager(final String namespace) {
        this.namespace = namespace;
    }

    /**
     * Upserts initial search attributes for a transfer.
     *
     * @param request the transfer request
     * @param transferId the transfer ID
     */
    void upsertInitialAttributes(final TransferRequest request, final Long transferId) {
        if (!shouldUpsertAttributes()) {
            return;
        }

        if (request.getAmount() == null) {
            return;
        }

        final Map<String, Object> attributes = buildInitialAttributes(request);
        Workflow.upsertSearchAttributes(attributes);

        Workflow.getLogger(SearchAttributesManager.class)
                .info("Search attributes upserted for transfer {}: amount={}, priority={}",
                        transferId, request.getAmount(), attributes.get("Priority"));
    }

    /**
     * Updates search attributes when transfer status changes.
     *
     * @param status the new status
     */
    void updateStatusAttribute(final TransferStatus status) {
        if (!shouldUpsertAttributes()) {
            return;
        }

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("TransferStatus", status.name());
        Workflow.upsertSearchAttributes(attributes);
    }

    /**
     * Checks if running in test environment.
     *
     * @return true if test environment, false otherwise
     */
    private boolean shouldUpsertAttributes() {
        return !TEST_NAMESPACE.equals(namespace) && !"unittest".equals(namespace.toLowerCase());
    }

    /**
     * Builds initial search attributes map.
     *
     * @param request the transfer request
     * @return attributes map
     */
    private Map<String, Object> buildInitialAttributes(final TransferRequest request) {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("TransferAmount", request.getAmount().doubleValue());
        attributes.put("TransferStatus", TransferStatus.INITIATED.name());
        attributes.put("Priority", calculatePriority(request.getAmount()));

        if (request.getSourceAccountNumber() != null) {
            attributes.put("SourceAccount", request.getSourceAccountNumber());
        }
        if (request.getDestinationAccountNumber() != null) {
            attributes.put("DestinationAccount", request.getDestinationAccountNumber());
        }
        if (request.getCurrency() != null) {
            attributes.put("Currency", request.getCurrency());
        }

        return attributes;
    }

    /**
     * Calculates priority based on transfer amount.
     *
     * @param amount the transfer amount
     * @return priority level (0-5)
     */
    private int calculatePriority(final BigDecimal amount) {
        if (amount == null) {
            return 0;
        }

        if (amount.compareTo(BigDecimal.valueOf(MAX_AMOUNT_FOR_PRIORITY)) >= 0) {
            return 5; // Highest priority (VIP)
        } else if (amount.compareTo(BigDecimal.valueOf(HIGH_AMOUNT_FOR_PRIORITY)) >= 0) {
            return 4; // High value
        } else if (amount.compareTo(BigDecimal.valueOf(MEDIUM_AMOUNT_FOR_PRIORITY)) >= 0) {
            return 3; // Medium-high
        } else if (amount.compareTo(BigDecimal.valueOf(STANDARD_AMOUNT_FOR_PRIORITY)) >= 0) {
            return 2; // Standard
        } else if (amount.compareTo(BigDecimal.valueOf(LOW_AMOUNT_FOR_PRIORITY)) >= 0) {
            return 1; // Small
        } else {
            return 0; // Micro transfers
        }
    }
}
