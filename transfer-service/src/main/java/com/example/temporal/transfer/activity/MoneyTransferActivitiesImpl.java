package com.example.temporal.transfer.activity;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.exception.ValidationException;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.transfer.client.AccountServiceClient;
import com.example.temporal.transfer.client.ValidationServiceClient;
import com.example.temporal.transfer.service.TransferPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyTransferActivitiesImpl implements MoneyTransferActivities {

    private final AccountServiceClient accountServiceClient;
    private final ValidationServiceClient validationServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransferPersistenceService transferPersistenceService;

    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    @Override
    public void validateTransfer(final TransferRequest request) {
        log.info("Validating transfer request: {}", request);
        try {
            validationServiceClient.validateTransfer(request);
            log.info("Transfer validation successful for request: {}", request);
        } catch (Exception e) {
            log.warn("Transfer validation failed for request: {} - Error: {}", request, e.getMessage());
            
            // Distinguir entre erros temporários e permanentes
            if (isTemporaryError(e)) {
                log.info("Temporary error detected, will retry: {}", e.getMessage());
                throw e; // Permite retry
            } else if (isValidationError(e)) {
                log.error("Business validation error, will not retry: {}", e.getMessage());
                throw new ValidationException("Validation failed: " + e.getMessage(), e);
            } else {
                log.warn("Unknown error, will retry: {}", e.getMessage());
                throw e; // Por segurança, permite retry para erros desconhecidos
            }
        }
    }
    
    private boolean isTemporaryError(final Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("connection refused") ||
               message.contains("timeout") ||
               message.contains("connection reset") ||
               message.contains("network") ||
               message.contains("unavailable") ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.SocketTimeoutException;
    }
    
    private boolean isValidationError(final Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("insufficient funds") ||
               message.contains("invalid account") ||
               message.contains("account not found") ||
               message.contains("invalid amount") ||
               message.contains("validation failed");
    }

    @Override
    public void lockAccounts(final String sourceAccountNumber, final String destinationAccountNumber) {
        log.info("Locking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        accountServiceClient.lockAccounts(sourceAccountNumber, destinationAccountNumber);
    }

    @Override
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        log.info("Debiting account {} amount {}", accountNumber, amount);
        accountServiceClient.debitAccount(accountNumber, amount);
    }

    @Override
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        //throw new RuntimeException("falha no credito");

        log.info("Crediting account {} amount {}", accountNumber, amount);
        accountServiceClient.creditAccount(accountNumber, amount);
    }

    @Override
    public void unlockAccounts(final String sourceAccountNumber, final String destinationAccountNumber) {
        log.info("Unlocking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        // In a real implementation, you would have an endpoint to unlock accounts
        // This is typically done automatically when the transaction is committed or rolled back
    }

    @Override
    public void compensateDebit(final String accountNumber, final BigDecimal amount) {
        log.info("Compensating debit for account {} amount {}", accountNumber, amount);
        accountServiceClient.creditAccount(accountNumber, amount); // Reverse the debit
    }

    @Override
    public void compensateCredit(final String accountNumber, final BigDecimal amount) {
        log.info("Compensating credit for account {} amount {}", accountNumber, amount);
        accountServiceClient.debitAccount(accountNumber, amount); // Reverse the credit
    }

    @Override
    public void notifyTransferInitiated(final Long transferId) {
        log.info("Notifying transfer initiated: {}", transferId);
        kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
            String.format("TRANSFER_INITIATED:%d", transferId));
    }

    @Override
    public void notifyTransferCompleted(final Long transferId) {
        log.info("Notifying transfer completed: {}", transferId);
        kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
            String.format("TRANSFER_COMPLETED:%d", transferId));
    }

    @Override
    public void notifyTransferFailed(final Long transferId, final String reason) {
        log.info("Notifying transfer failed: {} reason: {}", transferId, reason);
        kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
            String.format("TRANSFER_FAILED:%d:%s", transferId, reason));
    }

    @Override
    public void updateTransferStatus(final Long transferId, final String status) {
        log.info("Updating transfer {} status to {}", transferId, status);
        try {
            TransferStatus transferStatus = TransferStatus.valueOf(status);
            transferPersistenceService.updateTransferStatus(transferId, transferStatus);
        } catch (Exception e) {
            log.error("Error updating transfer status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update transfer status", e);
        }
    }

    @Override
    public void updateTransferStatusWithReason(final Long transferId, final String status, final String reason) {
        log.info("Updating transfer {} status to {} with reason: {}", transferId, status, reason);
        try {
            TransferStatus transferStatus = TransferStatus.valueOf(status);
            transferPersistenceService.updateTransferStatus(transferId, transferStatus, reason);
        } catch (Exception e) {
            log.error("Error updating transfer status with reason: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update transfer status", e);
        }
    }
}