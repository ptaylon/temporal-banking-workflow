package com.example.temporal.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    // ID da transferência (opcional - usado internamente)
    private Long transferId;

    // Idempotency key for ensuring request is processed only once
    private String idempotencyKey;

    // Timer configurations for delayed execution
    private Long delayInSeconds; // Delay before starting the transfer
    
    private Long timeoutInSeconds; // Timeout for the entire transfer
    
    private boolean allowCancelDuringDelay = true; // Can cancel during delay period?
}