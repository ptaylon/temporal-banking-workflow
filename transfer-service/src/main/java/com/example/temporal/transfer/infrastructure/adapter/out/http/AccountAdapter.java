package com.example.temporal.transfer.infrastructure.adapter.out.http;

import com.example.temporal.transfer.client.AccountServiceClient;
import com.example.temporal.transfer.domain.port.out.AccountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter for account service operations
 * Implements domain port using Feign client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountAdapter implements AccountPort {

    private final AccountServiceClient accountServiceClient;

    @Override
    public void lockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
        log.debug("Locking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        final LockAccountsRequest request = new LockAccountsRequest(sourceAccountNumber, destinationAccountNumber);
        accountServiceClient.lockAccounts(request);
    }

    @Override
    public void unlockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
        log.debug("Unlocking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        // Note: AccountServiceClient doesn't have unlock method yet
        // This is a placeholder for when it's implemented
        log.info("Account unlock requested for: {} and {}", sourceAccountNumber, destinationAccountNumber);
    }

    @Override
    public void debitAccount(String accountNumber, BigDecimal amount) {
        log.debug("Debiting account {} amount {}", accountNumber, amount);
        final OperationRequest request = new OperationRequest(amount, null);
        accountServiceClient.debitAccount(accountNumber, request);
    }

    @Override
    public void creditAccount(String accountNumber, BigDecimal amount) {
        log.debug("Crediting account {} amount {}", accountNumber, amount);
        final OperationRequest request = new OperationRequest(amount, null);
        accountServiceClient.creditAccount(accountNumber, request);
    }

    @Override
    public boolean accountExists(String accountNumber) {
        log.debug("Checking if account exists: {}", accountNumber);
        // Note: AccountServiceClient doesn't have getAccount method
        // This is a placeholder - always returns true for now
        log.warn("accountExists not implemented - returning true by default");
        return true;
    }

    @Override
    public BigDecimal getAccountBalance(String accountNumber) {
        log.debug("Getting balance for account: {}", accountNumber);
        // Note: AccountServiceClient doesn't have getAccount method
        // This is a placeholder - returns 0 for now
        log.warn("getAccountBalance not implemented - returning 0 by default");
        return BigDecimal.ZERO;
    }
}
