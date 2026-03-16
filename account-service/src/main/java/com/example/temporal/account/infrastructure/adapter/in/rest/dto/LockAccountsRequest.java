package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

import lombok.Data;

/**
 * Request DTO for locking multiple accounts.
 */
@Data
public class LockAccountsRequest {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
}
