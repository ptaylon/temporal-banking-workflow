package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for balance queries.
 */
@Data
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal balance;
}
