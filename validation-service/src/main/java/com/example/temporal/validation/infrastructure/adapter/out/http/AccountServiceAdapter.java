package com.example.temporal.validation.infrastructure.adapter.out.http;

import com.example.temporal.validation.client.AccountServiceClient;
import com.example.temporal.validation.domain.port.out.AccountServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adapter for account service HTTP client
 * Implements the AccountServicePort using Feign client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceAdapter implements AccountServicePort {

    private final AccountServiceClient accountServiceClient;

    @Override
    public Optional<AccountInfo> getAccount(String accountNumber) {
        try {
            var accountResponse = accountServiceClient.getAccount(accountNumber);
            
            if (accountResponse == null) {
                return Optional.empty();
            }

            return Optional.of(new AccountInfo(
                    accountResponse.getAccountNumber(),
                    accountResponse.getOwnerName(),
                    accountResponse.getBalance(),
                    accountResponse.getCurrency(),
                    true // Assume active since Account model doesn't have isActive field
            ));
        } catch (Exception e) {
            log.error("Error fetching account {}: {}", accountNumber, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean accountExists(String accountNumber) {
        return getAccount(accountNumber).isPresent();
    }

    @Override
    public Optional<BigDecimal> getBalance(String accountNumber) {
        return getAccount(accountNumber).map(AccountInfo::balance);
    }

    @Override
    public Optional<String> getCurrency(String accountNumber) {
        return getAccount(accountNumber).map(AccountInfo::currency);
    }
}
