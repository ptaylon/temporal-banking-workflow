package com.example.temporal.validation.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain model for Transfer Validation - framework independent
 * Represents the core business concept of a transfer validation
 */
@Value
@Builder
@With
public class TransferValidationDomain {
    Long id;
    String transferId;
    String sourceAccountNumber;
    String destinationAccountNumber;
    BigDecimal amount;
    String currency;
    ValidationResult validationResult;
    String rejectionReason;
    Integer fraudScore;
    LocalDateTime validatedAt;
    String idempotencyKey;

    /**
     * Validation result enum
     */
    public enum ValidationResult {
        APPROVED,
        REJECTED,
        PENDING
    }

    /**
     * Business validation: ensures validation data is consistent
     */
    public void validate() {
        if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Source account number cannot be null or empty");
        }
        if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination account number cannot be null or empty");
        }
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (fraudScore != null && (fraudScore < 0 || fraudScore > 100)) {
            throw new IllegalArgumentException("Fraud score must be between 0 and 100");
        }
    }

    /**
     * Business rule: is this validation approved?
     */
    public boolean isApproved() {
        return validationResult == ValidationResult.APPROVED;
    }

    /**
     * Business rule: is this validation rejected?
     */
    public boolean isRejected() {
        return validationResult == ValidationResult.REJECTED;
    }

    /**
     * Creates a new pending validation
     */
    public static TransferValidationDomain createPending(
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency,
            String idempotencyKey) {

        TransferValidationDomain validation = TransferValidationDomain.builder()
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(amount)
                .currency(currency)
                .validationResult(ValidationResult.PENDING)
                .idempotencyKey(idempotencyKey)
                .validatedAt(LocalDateTime.now())
                .build();

        validation.validate();
        return validation;
    }

    /**
     * Approves the validation - returns new immutable instance
     */
    public TransferValidationDomain approve() {
        return this.withValidationResult(ValidationResult.APPROVED)
                   .withValidatedAt(LocalDateTime.now());
    }

    /**
     * Rejects the validation with reason - returns new immutable instance
     */
    public TransferValidationDomain reject(String reason) {
        return this.withValidationResult(ValidationResult.REJECTED)
                   .withRejectionReason(reason)
                   .withValidatedAt(LocalDateTime.now());
    }
}
