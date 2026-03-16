package com.example.temporal.account.domain.port.in;

import java.math.BigDecimal;

/**
 * Input port for account operations (debit, credit, lock)
 */
public interface AccountOperationsUseCase {

    /**
     * Lock accounts for transfer (pessimistic locking)
     */
    void lockAccounts(String sourceAccountNumber, String destinationAccountNumber);

    /**
     * Debit an account
     */
    void debitAccount(String accountNumber, BigDecimal amount);

    /**
     * Credit an account
     */
    void creditAccount(String accountNumber, BigDecimal amount);

    /**
     * Get account balance
     */
    BigDecimal getBalance(String accountNumber);
}
