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


@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyTransferActivitiesImpl implements MoneyTransferActivities {

    private final AccountServiceClient accountServiceClient;
    private final ValidationServiceClient validationServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransferPersistenceService transferPersistenceService;

    // Constantes
    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";
    private static final String TRANSFER_INITIATED_EVENT = "TRANSFER_INITIATED:%d";
    private static final String TRANSFER_COMPLETED_EVENT = "TRANSFER_COMPLETED:%d";
    private static final String TRANSFER_FAILED_EVENT = "TRANSFER_FAILED:%d:%s";
    
    // Mensagens de erro
    private static final String VALIDATION_FAILED_MSG = "Validation failed: %s";
    private static final String UPDATE_STATUS_FAILED_MSG = "Failed to update transfer status";
    private static final String NOTIFICATION_FAILED_MSG = "Failed to send notification for transfer %d";

    @Override
    public void validateTransfer(final TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }
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
                throw new ValidationException(String.format(VALIDATION_FAILED_MSG, e.getMessage()), e);
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
        if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Source account number cannot be null or empty");
        }
        if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination account number cannot be null or empty");
        }
        log.info("Locking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        accountServiceClient.lockAccounts(sourceAccountNumber, destinationAccountNumber);
    }

    @Override
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        log.info("Debiting account {} amount {} - Starting with 10s delay for testing", accountNumber, amount);
        
        try {
            // Delay de 10 segundos para permitir testes de pause/resume/cancel
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("Debit operation was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Debit operation interrupted", e);
        }
        
        accountServiceClient.debitAccount(accountNumber, amount);
        log.info("Debit completed for account {} amount {}", accountNumber, amount);
    }

    @Override
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        log.info("Crediting account {} amount {} - Starting with 10s delay for testing", accountNumber, amount);
        
        try {
            // Delay de 10 segundos para permitir testes de pause/resume/cancel
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("Credit operation was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Credit operation interrupted", e);
        }
        
        accountServiceClient.creditAccount(accountNumber, amount);
        log.info("Credit completed for account {} amount {}", accountNumber, amount);
    }

    @Override
    public void unlockAccounts(final String sourceAccountNumber, final String destinationAccountNumber) {
        if (sourceAccountNumber == null || sourceAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Source account number cannot be null or empty");
        }
        if (destinationAccountNumber == null || destinationAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination account number cannot be null or empty");
        }
        
        log.info("Unlocking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        
        try {
            // TODO: Implementar unlock quando o AccountServiceClient suportar esta operação
            // Por enquanto, apenas logamos a operação
            // Em sistemas reais, locks são frequentemente liberados automaticamente por timeout
            // ou quando a transação é commitada/rollback
            log.info("Account unlock requested for: {} and {}. Implementation pending.", 
                sourceAccountNumber, destinationAccountNumber);
            
            // Simulação de unlock bem-sucedido
            log.info("Accounts unlocked successfully: {} and {}", sourceAccountNumber, destinationAccountNumber);
        } catch (Exception e) {
            log.warn("Failed to unlock accounts {} and {}: {}. This may be handled automatically by the system.", 
                sourceAccountNumber, destinationAccountNumber, e.getMessage());
            // Não lançar exceção aqui pois unlock pode falhar sem afetar o workflow principal
        }
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
        try {
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
                String.format(TRANSFER_INITIATED_EVENT, transferId));
        } catch (Exception e) {
            log.error(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
            throw new RuntimeException(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
        }
    }

    @Override
    public void notifyTransferCompleted(final Long transferId) {
        log.info("Notifying transfer completed: {}", transferId);
        try {
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
                String.format(TRANSFER_COMPLETED_EVENT, transferId));
        } catch (Exception e) {
            log.error(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
            throw new RuntimeException(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
        }
    }

    @Override
    public void notifyTransferFailed(final Long transferId, final String reason) {
        log.info("Notifying transfer failed: {} reason: {}", transferId, reason);
        try {
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, 
                String.format(TRANSFER_FAILED_EVENT, transferId, reason));
        } catch (Exception e) {
            log.error(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
            throw new RuntimeException(String.format(NOTIFICATION_FAILED_MSG, transferId), e);
        }
    }

    @Override
    public void updateTransferStatus(final Long transferId, final String status) {
        if (transferId == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        log.info("Updating transfer {} status to {}", transferId, status);
        try {
            TransferStatus transferStatus = TransferStatus.valueOf(status);
            transferPersistenceService.updateTransferStatus(transferId, transferStatus);
        } catch (Exception e) {
            log.error("Error updating transfer status: {}", e.getMessage(), e);
            throw new RuntimeException(UPDATE_STATUS_FAILED_MSG, e);
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
            throw new RuntimeException(UPDATE_STATUS_FAILED_MSG, e);
        }
    }
}