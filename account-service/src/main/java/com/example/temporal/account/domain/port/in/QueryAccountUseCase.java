package com.example.temporal.account.domain.port.in;

import com.example.temporal.account.domain.model.AccountDomain;

import java.util.List;
import java.util.Optional;

/**
 * Input port for querying accounts
 */
public interface QueryAccountUseCase {

    /**
     * Get account by account number
     */
    Optional<AccountDomain> getAccountByNumber(String accountNumber);

    /**
     * Get account by ID
     */
    Optional<AccountDomain> getAccountById(Long id);

    /**
     * Get multiple accounts
     */
    List<AccountDomain> getAccounts(List<String> accountNumbers);

    /**
     * Check if account exists
     */
    boolean accountExists(String accountNumber);
}
