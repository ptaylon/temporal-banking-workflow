package com.example.temporal.transfer.infrastructure.adapter.out.temporal;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.exception.ValidationException;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adapter for Temporal activities using domain ports
 * Bridges Temporal workflows with hexagonal architecture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyTransferActivitiesAdapter implements MoneyTransferActivities {

    private final ValidationPort validationPort;
    private final AccountPort accountPort;
    private final NotificationPort notificationPort;
    private final TransferPersistencePort persistencePort;

    @Override
    public void validateTransfer(final TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }

        log.info("Validating transfer request: {}", request);

        try {
            // Convert DTO to domain for validation
            TransferDomain transfer = toDomain(request);

            // Use domain port for validation
            validationPort.validateTransfer(transfer);

            log.info("Transfer validation successful for request: {}", request);

        } catch (ValidationException e) {
            log.error("Business validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Transfer validation failed: {}", e.getMessage());
            throw e; // Allow retry for temporary errors
        }
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
        accountPort.lockAccounts(sourceAccountNumber, destinationAccountNumber);
    }

    @Override
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        validateAccountOperation(accountNumber, amount);

        log.info("Debiting account {} amount {} - Starting with delay", accountNumber, amount);

        try {
            // Delay for testing pause/resume/cancel (as per original implementation)
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("Debit operation was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Debit operation interrupted", e);
        }

        accountPort.debitAccount(accountNumber, amount);
        log.info("Debit completed for account {} amount {}", accountNumber, amount);
    }

    @Override
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        validateAccountOperation(accountNumber, amount);

        log.info("Crediting account {} amount {} - Starting with delay", accountNumber, amount);

        try {
            // Delay for testing pause/resume/cancel (as per original implementation)
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            log.warn("Credit operation was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Credit operation interrupted", e);
        }

        accountPort.creditAccount(accountNumber, amount);
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
            accountPort.unlockAccounts(sourceAccountNumber, destinationAccountNumber);
            log.info("Accounts unlocked successfully: {} and {}", sourceAccountNumber, destinationAccountNumber);
        } catch (Exception e) {
            log.warn("Failed to unlock accounts {} and {}: {}",
                    sourceAccountNumber, destinationAccountNumber, e.getMessage());
            // Don't throw - unlock failure shouldn't fail the workflow
        }
    }

    @Override
    public void compensateDebit(final String accountNumber, final BigDecimal amount) {
        log.info("Compensating debit for account {} amount {}", accountNumber, amount);
        accountPort.creditAccount(accountNumber, amount); // Reverse the debit
    }

    @Override
    public void compensateCredit(final String accountNumber, final BigDecimal amount) {
        log.info("Compensating credit for account {} amount {}", accountNumber, amount);
        accountPort.debitAccount(accountNumber, amount); // Reverse the credit
    }

    @Override
    public void notifyTransferInitiated(final Long transferId) {
        log.info("Notifying transfer initiated: {}", transferId);
        notificationPort.notifyTransferInitiated(transferId);
    }

    @Override
    public void notifyTransferCompleted(final Long transferId) {
        log.info("Notifying transfer completed: {}", transferId);
        notificationPort.notifyTransferCompleted(transferId);
    }

    @Override
    public void notifyTransferFailed(final Long transferId, final String reason) {
        log.info("Notifying transfer failed: {} reason: {}", transferId, reason);
        notificationPort.notifyTransferFailed(transferId, reason);
    }

    @Override
    public void updateTransferStatus(final Long transferId, final TransferStatus status) {
        if (transferId == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        log.info("Updating transfer {} status to {}", transferId, status);

        try {
            TransferDomain transfer = persistencePort.findById(transferId)
                    .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

            TransferDomain updated = transfer.updateStatus(status);
            persistencePort.update(updated);

        } catch (Exception e) {
            log.error("Error updating transfer status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update transfer status", e);
        }
    }

    @Override
    public void updateTransferStatusWithReason(final Long transferId, final TransferStatus status, final String reason) {
        log.info("Updating transfer {} status to {} with reason: {}", transferId, status, reason);

        try {
            TransferDomain transfer = persistencePort.findById(transferId)
                    .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

            TransferDomain updated = transfer
                    .withStatus(status)
                    .withFailureReason(reason);

            persistencePort.update(updated);

        } catch (Exception e) {
            log.error("Error updating transfer status with reason: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update transfer status", e);
        }
    }

    // ========== HELPERS ==========

    private void validateAccountOperation(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private TransferDomain toDomain(TransferRequest request) {
        return TransferDomain.builder()
                .id(request.getTransferId())
                .sourceAccountNumber(request.getSourceAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
    }
}
