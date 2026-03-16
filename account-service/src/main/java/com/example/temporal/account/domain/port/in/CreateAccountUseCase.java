package com.example.temporal.account.domain.port.in;

import com.example.temporal.account.domain.model.AccountDomain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Input port for creating accounts
 */
public interface CreateAccountUseCase {

    /**
     * Creates a new account (idempotent)
     */
    CreateAccountResult createAccount(CreateAccountCommand command);

    @Value
    @Builder
    class CreateAccountCommand {
        String accountNumber;
        String ownerName;
        BigDecimal initialBalance;
        String currency;
        String idempotencyKey;

        public void validate() {
            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Account number is required");
            }
            if (ownerName == null || ownerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Owner name is required");
            }
            if (currency == null || currency.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency is required");
            }
        }
    }

    @Value
    @Builder
    class CreateAccountResult {
        Long accountId;
        String accountNumber;
        String status;
        String message;

        public static CreateAccountResult success(AccountDomain account) {
            return CreateAccountResult.builder()
                    .accountId(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .status("CREATED")
                    .message("Account created successfully")
                    .build();
        }

        public static CreateAccountResult error(String message) {
            return CreateAccountResult.builder()
                    .status("ERROR")
                    .message(message)
                    .build();
        }
    }
}
