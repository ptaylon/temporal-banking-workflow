package com.example.temporal.account.domain.service;

import com.example.temporal.common.annotation.Idempotent;
import com.example.temporal.account.domain.port.in.AccountOperationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for account operations with idempotency support
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountOperationService {

    private final AccountOperationsUseCase accountOperationsUseCase;

    /**
     * Debits an account with idempotency guarantee
     * Idempotency handled automatically by @Idempotent annotation
     */
    @Idempotent(key = "#idempotencyKey", operationType = "DEBIT", entityId = "#accountNumber")
    @Transactional
    public void debitWithIdempotency(
            String accountNumber,
            BigDecimal amount,
            String idempotencyKey) {

        // Apenas lógica de negócio - idempotência é automática
        accountOperationsUseCase.debitAccount(accountNumber, amount);
    }

    /**
     * Credits an account with idempotency guarantee
     * Idempotency handled automatically by @Idempotent annotation
     */
    @Idempotent(key = "#idempotencyKey", operationType = "CREDIT", entityId = "#accountNumber")
    @Transactional
    public void creditWithIdempotency(
            String accountNumber,
            BigDecimal amount,
            String idempotencyKey) {

        // Apenas lógica de negócio - idempotência é automática
        accountOperationsUseCase.creditAccount(accountNumber, amount);
    }

    /**
     * Locks accounts (no idempotency needed for this operation)
     */
    @Transactional
    public void lockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
        accountOperationsUseCase.lockAccounts(sourceAccountNumber, destinationAccountNumber);
    }

    /**
     * Gets account balance (read operation, no idempotency needed)
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber) {
        return accountOperationsUseCase.getBalance(accountNumber);
    }
}
