package com.example.temporal.validation.client;

import com.example.temporal.common.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for AccountServiceClient
 * Returns null when account service is unavailable
 */
@Slf4j
@Component
public class AccountServiceClientFallback implements AccountServiceClient {

    @Override
    public Account getAccount(String accountNumber) {
        log.error("Account service unavailable, returning null for account: {}", accountNumber);
        return null;
    }
}
