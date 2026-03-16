package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for account operations (debit/credit).
 */
@Data
public class OperationRequest {
    private BigDecimal amount;
}
