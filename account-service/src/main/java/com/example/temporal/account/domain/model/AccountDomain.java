package com.example.temporal.account.domain.model;

import com.example.temporal.common.message.ErrorMessages;
import com.example.temporal.common.message.MessageResolver;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pure domain model for Account - framework independent
 * Represents the core business concept of a bank account
 * <p>
 * This is a self-validating immutable domain object.
 * All constructor/factory methods automatically validate the data.
 * </p>
 */
@Value
@Builder
@With
public class AccountDomain {
    Long id;
    String accountNumber;
    String ownerName;
    BigDecimal balance;
    String currency;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String idempotencyKey;

    /**
     * Business validation: ensures account has valid data.
     * This method is automatically called during object creation.
     *
     * @throws IllegalArgumentException if any field is invalid
     */
    public void validate() {
        validateAccountNumber(this.accountNumber);
        validateOwnerName(this.ownerName);
        validateBalance(this.balance);
        validateCurrency(this.currency);
    }

    /**
     * Validates account number.
     *
     * @param accountNumber the account number to validate
     * @throws IllegalArgumentException if account number is invalid
     */
    private static void validateAccountNumber(final String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.ACCOUNT_NUMBER_REQUIRED));
        }
    }

    /**
     * Validates owner name.
     *
     * @param ownerName the owner name to validate
     * @throws IllegalArgumentException if owner name is invalid
     */
    private static void validateOwnerName(final String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.OWNER_NAME_REQUIRED));
        }
    }

    /**
     * Validates balance.
     *
     * @param balance the balance to validate
     * @throws IllegalArgumentException if balance is invalid
     */
    private static void validateBalance(final BigDecimal balance) {
        if (balance == null) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.BALANCE_CANNOT_BE_NULL));
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.BALANCE_CANNOT_BE_NEGATIVE));
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
     * Business rule: can debit this amount?
     *
     * @param amount the amount to debit
     * @return true if debit is possible, false otherwise
     */
    public boolean canDebit(final BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return this.balance.compareTo(amount) >= 0;
    }

    /**
     * Business operation: debit account.
     * Returns a new immutable instance with updated balance.
     *
     * @param amount the amount to debit
     * @return new AccountDomain instance with debited balance
     * @throws IllegalArgumentException if amount is invalid
     * @throws IllegalStateException    if insufficient funds
     */
    public AccountDomain debit(final BigDecimal amount) {
        validateDebitAmount(amount);

        if (!canDebit(amount)) {
            throw new IllegalStateException(
                    MessageResolver.resolveError(ErrorMessages.INSUFFICIENT_FUNDS,
                            this.balance, amount));
        }

        return this.withBalance(this.balance.subtract(amount))
                   .withUpdatedAt(LocalDateTime.now());
    }

    /**
     * Validates debit amount.
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is invalid
     */
    private void validateDebitAmount(final BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    /**
     * Business operation: credit account.
     * Returns a new immutable instance with updated balance.
     *
     * @param amount the amount to credit
     * @return new AccountDomain instance with credited balance
     * @throws IllegalArgumentException if amount is invalid
     */
    public AccountDomain credit(final BigDecimal amount) {
        validateCreditAmount(amount);

        return this.withBalance(this.balance.add(amount))
                   .withUpdatedAt(LocalDateTime.now());
    }

    /**
     * Validates credit amount.
     *
     * @param amount the amount to validate
     * @throws IllegalArgumentException if amount is invalid
     */
    private void validateCreditAmount(final BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
    }

    /**
     * Creates a new validated account.
     *
     * @param accountNumber   the account number
     * @param ownerName       the owner name
     * @param initialBalance  the initial balance (defaults to 0 if null)
     * @param currency        the currency
     * @param idempotencyKey  the idempotency key
     * @return new validated AccountDomain instance
     * @throws IllegalArgumentException if any field is invalid
     */
    public static AccountDomain create(
            final String accountNumber,
            final String ownerName,
            final BigDecimal initialBalance,
            final String currency,
            final String idempotencyKey) {

        final AccountDomain account = AccountDomain.builder()
                .accountNumber(accountNumber)
                .ownerName(ownerName)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .currency(currency)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        account.validate();
        return account;
    }
}
