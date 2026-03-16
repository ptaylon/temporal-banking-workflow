package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for creating an account
 */
@Data
public class AccountCreateRequest {
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;
    private String currency;
    private String idempotencyKey;
}
