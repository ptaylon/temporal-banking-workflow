package com.example.temporal.account.domain.port.out;

import com.example.temporal.account.domain.model.AccountDomain;

import java.util.List;
import java.util.Optional;

/**
 * Output port for account persistence
 */
public interface AccountPersistencePort {

    /**
     * Save a new account
     */
    AccountDomain save(AccountDomain account);

    /**
     * Update an existing account
     */
    AccountDomain update(AccountDomain account);

    /**
     * Find account by account number
     */
    Optional<AccountDomain> findByAccountNumber(String accountNumber);

    /**
     * Find account by account number with pessimistic lock
     */
    Optional<AccountDomain> findByAccountNumberWithLock(String accountNumber);

    /**
     * Find account by idempotency key
     */
    Optional<AccountDomain> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find account by ID
     */
    Optional<AccountDomain> findById(Long id);

    /**
     * Find multiple accounts
     */
    List<AccountDomain> findByAccountNumberIn(List<String> accountNumbers);

    /**
     * Check if account exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Check if idempotency key exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}
