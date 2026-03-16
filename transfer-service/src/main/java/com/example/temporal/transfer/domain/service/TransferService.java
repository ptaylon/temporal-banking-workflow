package com.example.temporal.transfer.domain.service;

import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.in.InitiateTransferUseCase;
import com.example.temporal.transfer.domain.port.in.QueryTransferUseCase;
import com.example.temporal.transfer.domain.port.out.TransferPersistencePort;
import com.example.temporal.transfer.domain.port.out.WorkflowOrchestrationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service implementing transfer use cases
 * Contains pure business logic without framework dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService implements InitiateTransferUseCase, QueryTransferUseCase {

    private final TransferPersistencePort persistencePort;
    private final WorkflowOrchestrationPort orchestrationPort;

    @Override
    @Transactional
    public TransferInitiationResult initiateTransfer(InitiateTransferCommand command) {
        log.info("Initiating transfer: {}", command);

        try {
            // Validate command
            command.validate();

            // Generate idempotency key if not provided
            String idempotencyKey = command.getIdempotencyKey() != null
                ? command.getIdempotencyKey()
                : UUID.randomUUID().toString();

            // Check for duplicate request (idempotency)
            Optional<TransferDomain> existingTransfer = persistencePort.findByIdempotencyKey(idempotencyKey);
            if (existingTransfer.isPresent()) {
                log.info("Transfer already exists for idempotency key: {}", idempotencyKey);
                TransferDomain existing = existingTransfer.get();
                String workflowId = "transfer-" + existing.getId();
                return TransferInitiationResult.success(existing.getId(), workflowId);
            }

            // Create domain transfer object
            TransferDomain transfer = TransferDomain.initiate(
                command.getSourceAccountNumber(),
                command.getDestinationAccountNumber(),
                command.getAmount(),
                command.getCurrency(),
                idempotencyKey
            );

            // Persist transfer
            TransferDomain savedTransfer = persistencePort.save(transfer);

            // Generate workflow ID
            String workflowId = "transfer-" + savedTransfer.getId();

            // Start workflow asynchronously
            orchestrationPort.startTransferWorkflow(savedTransfer, workflowId);

            log.info("Transfer initiated successfully with ID: {} and workflowId: {}",
                savedTransfer.getId(), workflowId);

            return TransferInitiationResult.success(savedTransfer.getId(), workflowId);

        } catch (IllegalArgumentException e) {
            log.error("Validation error initiating transfer: {}", e.getMessage());
            return TransferInitiationResult.error("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error initiating transfer: {}", e.getMessage(), e);
            return TransferInitiationResult.error("Failed to initiate transfer: " + e.getMessage());
        }
    }

    @Override
    public Optional<TransferDomain> getTransferById(Long transferId) {
        log.debug("Getting transfer by ID: {}", transferId);
        return persistencePort.findById(transferId);
    }

    @Override
    public Optional<TransferDomain> getTransferByWorkflowId(String workflowId) {
        log.debug("Getting transfer by workflow ID: {}", workflowId);

        try {
            // Extract transfer ID from workflow ID (format: "transfer-123")
            Long transferId = Long.parseLong(workflowId.replace("transfer-", ""));
            return persistencePort.findById(transferId);
        } catch (NumberFormatException e) {
            log.error("Invalid workflow ID format: {}", workflowId);
            return Optional.empty();
        }
    }

    @Override
    public List<TransferDomain> getTransfersByAccount(String accountNumber) {
        log.debug("Getting transfers by account: {}", accountNumber);
        return persistencePort.findByAccountNumber(accountNumber);
    }

    @Override
    public TransferStatusResult getTransferStatus(String workflowId) {
        log.debug("Getting transfer status for workflow: {}", workflowId);

        try {
            // Get transfer from database (source of truth for persisted data)
            Optional<TransferDomain> transferOpt = getTransferByWorkflowId(workflowId);

            if (transferOpt.isEmpty()) {
                throw new IllegalArgumentException("Transfer not found for workflow: " + workflowId);
            }

            TransferDomain transfer = transferOpt.get();

            // Try to get workflow status if workflow is still running
            WorkflowOrchestrationPort.WorkflowStatus workflowStatus = null;
            boolean isWorkflowRunning = false;

            try {
                isWorkflowRunning = orchestrationPort.isWorkflowRunning(workflowId);
                if (isWorkflowRunning) {
                    workflowStatus = orchestrationPort.getWorkflowStatus(workflowId);
                }
            } catch (Exception e) {
                log.debug("Could not query workflow status (workflow may be completed): {}", e.getMessage());
            }

            return TransferStatusResult.builder()
                    .transfer(transfer)
                    .workflowStatus(workflowStatus != null ? workflowStatus.getStatus() : transfer.getStatus().name())
                    .isWorkflowRunning(isWorkflowRunning)
                    .build();

        } catch (Exception e) {
            log.error("Error getting transfer status for workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Error retrieving transfer status: " + e.getMessage(), e);
        }
    }
}
