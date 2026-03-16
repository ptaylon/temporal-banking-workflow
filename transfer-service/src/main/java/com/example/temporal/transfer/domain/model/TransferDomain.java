package com.example.temporal.transfer.domain.model;

import com.example.temporal.common.message.ErrorMessages;
import com.example.temporal.common.message.MessageResolver;
import com.example.temporal.common.model.TransferStatus;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain model for Transfer - framework independent
 * Represents the core business concept of a money transfer
 * <p>
 * This is a self-validating immutable domain object.
 * All constructor/factory methods automatically validate the data.
 * </p>
 */
@Value
@Builder
@With
public class TransferDomain {
    Long id;
    String sourceAccountNumber;
    String destinationAccountNumber;
    BigDecimal amount;
    String currency;
    TransferStatus status;
    String failureReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String idempotencyKey; // For idempotent operations

    /**
     * Business validation: ensures transfer has valid data.
     * This method is automatically called during object creation.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public void validate() {
        validateSourceAccount(this.sourceAccountNumber);
        validateDestinationAccount(this.destinationAccountNumber);
        validateAccountsNotSame(this.sourceAccountNumber, this.destinationAccountNumber);
        validateAmount(this.amount);
        validateCurrency(this.currency);
    }

    /**
     * Validates source account number.
     *
     * @param sourceAccountNumber the source account number to validate
     * @throws IllegalArgumentException if source account is invalid
     */
    private static void validateSourceAccount(final String sourceAccountNumber) {
        if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.SOURCE_ACCOUNT_REQUIRED));
        }
    }

    /**
     * Validates destination account number.
     *
     * @param destinationAccountNumber the destination account number to validate
     * @throws IllegalArgumentException if destination account is invalid
     */
    private static void validateDestinationAccount(final String destinationAccountNumber) {
        if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.DESTINATION_ACCOUNT_REQUIRED));
        }
    }

    /**
     * Validates that source and destination accounts are not the same.
     *
     * @param sourceAccountNumber      the source account number
     * @param destinationAccountNumber the destination account number
     * @throws IllegalArgumentException if accounts are the same
     */
    private static void validateAccountsNotSame(
            final String sourceAccountNumber,
            final String destinationAccountNumber) {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.ACCOUNTS_CANNOT_BE_SAME));
        }
    }

    /**
     * Validates amount.
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is invalid
     */
    private static void validateAmount(final BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.AMOUNT_MUST_BE_POSITIVE));
        }
    }

    /**
     * Validates currency.
     *
     * @param currency the currency to validate
     * @throws IllegalArgumentException if currency is invalid
     */
    private static void validateCurrency(final String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.CURRENCY_REQUIRED));
        }
    }

    /**
     * Business rule: can this transfer be cancelled?
     *
     * @return true if transfer can be cancelled, false otherwise
     */
    public boolean canBeCancelled() {
        return this.status == TransferStatus.INITIATED ||
               this.status == TransferStatus.VALIDATED;
    }

    /**
     * Business rule: can this transfer be paused?
     *
     * @return true if transfer can be paused, false otherwise
     */
    public boolean canBePaused() {
        return this.status == TransferStatus.INITIATED ||
               this.status == TransferStatus.VALIDATED;
    }

    /**
     * Business rule: is this transfer in a final state?
     *
     * @return true if transfer is in final state, false otherwise
     */
    public boolean isInFinalState() {
        return this.status == TransferStatus.COMPLETED ||
               this.status == TransferStatus.FAILED ||
               this.status == TransferStatus.CANCELLED ||
               this.status == TransferStatus.COMPENSATED;
    }

    /**
     * Creates a new validated transfer with INITIATED status.
     *
     * @param sourceAccountNumber      the source account number
     * @param destinationAccountNumber the destination account number
     * @param amount                   the transfer amount
     * @param currency                 the currency
     * @param idempotencyKey           the idempotency key
     * @return new validated TransferDomain instance
     * @throws IllegalArgumentException if any field is invalid
     */
    public static TransferDomain initiate(
            final String sourceAccountNumber,
            final String destinationAccountNumber,
            final BigDecimal amount,
            final String currency,
            final String idempotencyKey) {

        final TransferDomain transfer = TransferDomain.builder()
                .sourceAccountNumber(sourceAccountNumber)
                .destinationAccountNumber(destinationAccountNumber)
                .amount(amount)
                .currency(currency)
                .status(TransferStatus.INITIATED)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transfer.validate();
        return transfer;
    }

    /**
     * Updates status - returns new immutable instance.
     *
     * @param newStatus the new status
     * @return new TransferDomain instance with updated status
     */
    public TransferDomain updateStatus(final TransferStatus newStatus) {
        return this.withStatus(newStatus)
                   .withUpdatedAt(LocalDateTime.now());
    }

    /**
     * Updates status to FAILED with failure reason - returns new immutable instance.
     *
     * @param reason the failure reason
     * @return new TransferDomain instance with failed status
     */
    public TransferDomain fail(final String reason) {
        return this.withStatus(TransferStatus.FAILED)
                   .withFailureReason(reason)
                   .withUpdatedAt(LocalDateTime.now());
    }
}
