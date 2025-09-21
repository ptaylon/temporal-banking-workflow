package com.example.temporal.common.model;

public enum TransferStatus {
    INITIATED,
    VALIDATING,
    VALIDATED,
    PROCESSING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}