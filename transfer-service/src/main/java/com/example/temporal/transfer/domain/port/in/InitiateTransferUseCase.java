package com.example.temporal.transfer.domain.port.in;

import com.example.temporal.transfer.domain.model.TransferDomain;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Input port (driving port) for initiating transfers
 * Defines the contract for the use case without implementation details
 */
public interface InitiateTransferUseCase {

    /**
     * Initiates a new money transfer
     * @param command transfer details
     * @return initiated transfer with workflow ID
     */
    TransferInitiationResult initiateTransfer(InitiateTransferCommand command);

    /**
     * Command object for transfer initiation
     */
    @Value
    @Builder
    class InitiateTransferCommand {
        String sourceAccountNumber;
        String destinationAccountNumber;
        BigDecimal amount;
        String currency;
        String idempotencyKey; // For idempotent operations

        public void validate() {
            if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Source account number is required");
            }
            if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination account number is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            if (currency == null || currency.trim().isEmpty()) {
                throw new IllegalArgumentException("Currency is required");
            }
        }
    }

    /**
     * Result object for transfer initiation
     */
    @Value
    @Builder
    class TransferInitiationResult {
        Long transferId;
        String workflowId;
        String status;
        String message;

        public static TransferInitiationResult success(Long transferId, String workflowId) {
            return TransferInitiationResult.builder()
                    .transferId(transferId)
                    .workflowId(workflowId)
                    .status("INITIATED")
                    .message("Transfer initiated successfully")
                    .build();
        }

        public static TransferInitiationResult error(String message) {
            return TransferInitiationResult.builder()
                    .status("ERROR")
                    .message(message)
                    .build();
        }
    }
}
