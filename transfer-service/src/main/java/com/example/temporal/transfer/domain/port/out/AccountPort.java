package com.example.temporal.transfer.domain.port.out;

import java.math.BigDecimal;

/**
 * Output port (driven port) for account operations
 * Defines contract for interacting with account service
 */
public interface AccountPort {

    /**
     * Lock accounts for transfer
     */
    void lockAccounts(String sourceAccountNumber, String destinationAccountNumber);

    /**
     * Unlock accounts
     */
    void unlockAccounts(String sourceAccountNumber, String destinationAccountNumber);

    /**
     * Debit an account
     */
    void debitAccount(String accountNumber, BigDecimal amount);

    /**
     * Credit an account
     */
    void creditAccount(String accountNumber, BigDecimal amount);

    /**
     * Check if account exists
     */
    boolean accountExists(String accountNumber);

    /**
     * Get account balance (for validation)
     */
    BigDecimal getAccountBalance(String accountNumber);
}
