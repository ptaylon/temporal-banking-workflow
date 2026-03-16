package com.example.temporal.validation.domain.port.in;

import com.example.temporal.validation.domain.model.TransferValidationDomain;

/**
 * Use case for validating transfers
 * Defines what the system can do regarding transfer validation
 */
public interface ValidateTransferUseCase {

    /**
     * Validates a transfer request
     * @param command the validation command
     * @return the validation result
     */
    ValidationResult validateTransfer(ValidateTransferCommand command);

    /**
     * Command object for transfer validation
     */
    record ValidateTransferCommand(
            String sourceAccountNumber,
            String destinationAccountNumber,
            java.math.BigDecimal amount,
            String currency,
            String idempotencyKey
    ) {
        public ValidateTransferCommand {
            if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Source account number cannot be null or empty");
            }
            if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination account number cannot be null or empty");
            }
            if (sourceAccountNumber.equals(destinationAccountNumber)) {
                throw new IllegalArgumentException("Source and destination accounts cannot be the same");
            }
            if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            if (currency == null || currency.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency cannot be null or empty");
            }
        }

        public static ValidateTransferCommand of(
                String sourceAccountNumber,
                String destinationAccountNumber,
                java.math.BigDecimal amount,
                String currency,
                String idempotencyKey) {
            return new ValidateTransferCommand(
                    sourceAccountNumber,
                    destinationAccountNumber,
                    amount,
                    currency,
                    idempotencyKey
            );
        }
    }

    /**
     * Result object for validation
     */
    record ValidationResult(
            boolean approved,
            String rejectionReason,
            Integer fraudScore,
            Long validationId
    ) {
        public static ValidationResult approved(Long validationId, Integer fraudScore) {
            return new ValidationResult(true, null, fraudScore, validationId);
        }

        public static ValidationResult rejected(Long validationId, String reason) {
            return new ValidationResult(false, reason, null, validationId);
        }
    }
}
