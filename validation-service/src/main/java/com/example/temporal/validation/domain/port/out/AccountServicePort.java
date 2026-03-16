package com.example.temporal.validation.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port for account service operations
 * Defines what the domain needs from the external account service
 */
public interface AccountServicePort {

    /**
     * Gets account information
     * @param accountNumber the account number
     * @return account information if found
     */
    Optional<AccountInfo> getAccount(String accountNumber);

    /**
     * Checks if account exists
     * @param accountNumber the account number
     * @return true if account exists
     */
    boolean accountExists(String accountNumber);

    /**
     * Gets account balance
     * @param accountNumber the account number
     * @return the balance if account exists
     */
    Optional<BigDecimal> getBalance(String accountNumber);

    /**
     * Gets account currency
     * @param accountNumber the account number
     * @return the currency if account exists
     */
    Optional<String> getCurrency(String accountNumber);

    /**
     * Account information DTO
     */
    record AccountInfo(
            String accountNumber,
            String ownerName,
            BigDecimal balance,
            String currency,
            boolean active
    ) {}
}
