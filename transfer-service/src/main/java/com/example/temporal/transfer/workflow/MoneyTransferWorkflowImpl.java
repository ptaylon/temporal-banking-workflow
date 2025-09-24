package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.common.dto.TransferControlAction;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import com.example.temporal.common.exception.ValidationException;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

public class MoneyTransferWorkflowImpl implements MoneyTransferWorkflow {

    private TransferResponse currentResponse;
    
    // Campos de controle para signals e queries
    private boolean paused = false;
    private boolean cancelled = false;
    private String pauseReason;
    private String cancelReason;
    private TransferControlAction lastControlAction;
    private LocalDateTime lastControlTimestamp;

    // Activity stubs with specific configurations
    private final MoneyTransferActivities validationActivities = Workflow.newActivityStub(MoneyTransferActivities.class,
            createValidationActivityOptions());

    private final MoneyTransferActivities accountActivities = Workflow.newActivityStub(MoneyTransferActivities.class,
            createAccountActivityOptions());

    private final MoneyTransferActivities notificationActivities = Workflow
            .newActivityStub(MoneyTransferActivities.class, createNotificationActivityOptions());

    private final MoneyTransferActivities persistenceActivities = Workflow
            .newActivityStub(MoneyTransferActivities.class, createPersistenceActivityOptions());

    /**
     * Creates activity options for validation operations with extended retry policy
     */
    private ActivityOptions createValidationActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setMaximumInterval(Duration.ofMinutes(5))
                        .setBackoffCoefficient(2.0)
                        .setMaximumAttempts(20) // Extended retries for connectivity issues
                        .setDoNotRetry(ValidationException.class.getName())
                        .build())
                .build();
    }

    /**
     * Creates activity options for account operations with moderate retry policy
     */
    private ActivityOptions createAccountActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofMinutes(2))
                        .setBackoffCoefficient(2.0)
                        .setMaximumAttempts(15) // Critical banking operations
                        .build())
                .build();
    }

    /**
     * Creates activity options for notification operations with quick retry policy
     */
    private ActivityOptions createNotificationActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setBackoffCoefficient(2.0)
                        .setMaximumAttempts(10)
                        .build())
                .build();
    }

    /**
     * Creates activity options for persistence operations with extended retry
     * policy
     */
    private ActivityOptions createPersistenceActivityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumInterval(Duration.ofSeconds(10))
                        .setBackoffCoefficient(2.0)
                        .setMaximumAttempts(15) // Important for data consistency
                        .build())
                .build();
    }

    @Override
    public TransferResponse executeTransfer(TransferRequest transferRequest) {
        Long transferId = generateTransferId(transferRequest);
        Saga saga = createSaga();

        initializeTransferResponse(transferRequest, transferId);

        try {
            // Verificar se foi cancelado antes de iniciar
            checkCancellation();
            
            initializeTransfer(transferId);
            
            // Aguardar se pausado após inicialização
            waitIfPaused();
            checkCancellation();
            
            validateTransfer(transferRequest, transferId);
            
            // Aguardar se pausado após validação
            waitIfPaused();
            checkCancellation();
            
            executeAccountOperations(transferRequest, saga, transferId);
            
            // Aguardar se pausado após operações de conta
            waitIfPaused();
            checkCancellation();
            
            completeTransfer(transferId);
        } catch (ActivityFailure e) {
            handleTransferFailure(transferId, e);
            throw e;
        }

        return currentResponse;
    }

    /**
     * Generates a transfer ID from request or creates a new random one
     */
    private Long generateTransferId(TransferRequest transferRequest) {
        return transferRequest.getTransferId() != null ? transferRequest.getTransferId() : new Random().nextLong();
    }

    /**
     * Creates a Saga instance for compensation management
     */
    private Saga createSaga() {
        return new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build());
    }

    /**
     * Initializes the transfer response object
     */
    private void initializeTransferResponse(TransferRequest transferRequest, Long transferId) {
        currentResponse = new TransferResponse()
                .setTransferId(transferId)
                .setSourceAccountNumber(transferRequest.getSourceAccountNumber())
                .setDestinationAccountNumber(transferRequest.getDestinationAccountNumber())
                .setAmount(transferRequest.getAmount())
                .setCurrency(transferRequest.getCurrency())
                .setStatus(TransferStatus.INITIATED);
    }

    /**
     * Sets initial transfer status and sends notifications
     */
    private void initializeTransfer(Long transferId) {
        currentResponse.setStatus(TransferStatus.INITIATED);
        persistenceActivities.updateTransferStatus(transferId, TransferStatus.INITIATED.name());
        notificationActivities.notifyTransferInitiated(transferId);
    }

    /**
     * Validates the transfer request with enhanced error handling
     */
    private void validateTransfer(TransferRequest transferRequest, Long transferId) {
        try {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Starting validation for transfer ID: {} - Will retry up to 20 times for connectivity issues",
                            transferId);

            validationActivities.validateTransfer(transferRequest);

            currentResponse.setStatus(TransferStatus.VALIDATED);
            persistenceActivities.updateTransferStatus(transferId, TransferStatus.VALIDATED.name());

            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer validation successful for ID: {}", transferId);

        } catch (ActivityFailure e) {
            handleValidationFailure(transferId, e);
            throw e;
        }
    }

    /**
     * Handles validation failure with proper logging and status updates
     */
    private void handleValidationFailure(Long transferId, ActivityFailure e) {
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .error("Transfer validation failed permanently for ID: {} after all retries. Error: {}",
                        transferId, e.getMessage());

        currentResponse.setStatus(TransferStatus.FAILED);
        String truncatedError = truncateErrorMessage(e.getMessage());
        persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.FAILED.name(), truncatedError);
        notificationActivities.notifyTransferFailed(transferId, truncatedError);
    }

    /**
     * Executes account operations (lock, debit, credit) with saga compensation
     */
    private void executeAccountOperations(TransferRequest transferRequest, Saga saga, Long transferId) {
        lockAccountsWithCompensation(transferRequest, saga);
        debitAccountWithCompensation(transferRequest, saga);
        creditAccountWithCompensation(transferRequest, saga, transferId);
    }

    /**
     * Locks accounts and registers compensation
     */
    private void lockAccountsWithCompensation(TransferRequest transferRequest, Saga saga) {
        accountActivities.lockAccounts(
                transferRequest.getSourceAccountNumber(),
                transferRequest.getDestinationAccountNumber());
        saga.addCompensation(accountActivities::unlockAccounts,
                transferRequest.getSourceAccountNumber(),
                transferRequest.getDestinationAccountNumber());
    }

    /**
     * Debits source account and registers compensation
     */
    private void debitAccountWithCompensation(TransferRequest transferRequest, Saga saga) {
        accountActivities.debitAccount(
                transferRequest.getSourceAccountNumber(),
                transferRequest.getAmount());
        saga.addCompensation(accountActivities::compensateDebit,
                transferRequest.getSourceAccountNumber(),
                transferRequest.getAmount());
    }

    /**
     * Credits destination account with error handling and compensation
     */
    private void creditAccountWithCompensation(TransferRequest transferRequest, Saga saga, Long transferId) {
        try {
            accountActivities.creditAccount(
                    transferRequest.getDestinationAccountNumber(),
                    transferRequest.getAmount());
            // Only add compensation AFTER successful credit
            saga.addCompensation(accountActivities::compensateCredit,
                    transferRequest.getDestinationAccountNumber(),
                    transferRequest.getAmount());
        } catch (ActivityFailure e) {
            handleCreditFailureWithCompensation(saga, transferId, e);
            throw e;
        }
    }

    /**
     * Handles credit failure by executing compensation
     */
    private void handleCreditFailureWithCompensation(Saga saga, Long transferId, ActivityFailure e) {
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .warn("Credit account failed for transfer ID: {}, starting compensation. Error: {}",
                        transferId, e.getMessage());

        currentResponse.setStatus(TransferStatus.COMPENSATING);
        currentResponse.setFailureReason(e.getMessage());
        persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.COMPENSATING.name(),
                e.getMessage());

        // Execute compensation (debit + unlock only, no credit to compensate)
        saga.compensate();

        currentResponse.setStatus(TransferStatus.COMPENSATED);
        persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.COMPENSATED.name(),
                e.getMessage());
        notificationActivities.notifyTransferFailed(transferId, e.getMessage());

        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Compensation completed for transfer ID: {}", transferId);
    }

    /**
     * Completes the transfer with success status and notifications
     */
    private void completeTransfer(Long transferId) {
        currentResponse.setStatus(TransferStatus.COMPLETED);
        persistenceActivities.updateTransferStatus(transferId, TransferStatus.COMPLETED.name());
        notificationActivities.notifyTransferCompleted(transferId);
    }

    /**
     * Handles general transfer failures that weren't handled in specific steps
     */
    private void handleTransferFailure(Long transferId, ActivityFailure e) {
        if (currentResponse.getStatus() != TransferStatus.FAILED &&
                currentResponse.getStatus() != TransferStatus.COMPENSATED) {
            currentResponse.setStatus(TransferStatus.FAILED);
            persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.FAILED.name(),
                    e.getMessage());
            notificationActivities.notifyTransferFailed(transferId, e.getMessage());
        }
    }

    /**
     * Truncates error messages to prevent database field overflow
     */
    private String truncateErrorMessage(String errorMessage) {
        return errorMessage.length() > 200 ? errorMessage.substring(0, 200) + "..." : errorMessage;
    }

    /**
     * Aguarda enquanto o workflow estiver pausado
     */
    private void waitIfPaused() {
        if (paused) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer paused, waiting for resume signal. WorkflowId: {}", 
                          Workflow.getInfo().getWorkflowId());
        }
        Workflow.await(() -> !paused);
    }

    /**
     * Verifica se o workflow foi cancelado e lança exceção se necessário
     */
    private void checkCancellation() {
        if (cancelled) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer cancelled. Reason: {}. WorkflowId: {}", 
                          cancelReason, Workflow.getInfo().getWorkflowId());
            throw new RuntimeException("Transfer cancelled: " + cancelReason);
        }
    }

    @Override
    public TransferResponse getStatus() {
        return currentResponse;
    }

    // Signal Methods para controle de transferência
    @Override
    public void pauseTransfer() {
        // Verificar se a funcionalidade está habilitada via side effect
        boolean controlEnabled = Workflow.sideEffect(Boolean.class, () -> {
            // Em um cenário real, isso viria de uma configuração externa
            // Por enquanto, assumimos que está habilitado
            return true;
        });
        
        if (!controlEnabled) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Pause functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        
        this.paused = true;
        this.lastControlAction = TransferControlAction.PAUSE;
        this.lastControlTimestamp = LocalDateTime.now();
        
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer paused via signal. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
    }

    @Override
    public void resumeTransfer() {
        // Verificar se a funcionalidade está habilitada
        boolean controlEnabled = Workflow.sideEffect(Boolean.class, () -> true);
        
        if (!controlEnabled) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Resume functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        
        this.paused = false;
        this.pauseReason = null;
        this.lastControlAction = TransferControlAction.RESUME;
        this.lastControlTimestamp = LocalDateTime.now();
        
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer resumed via signal. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
    }

    @Override
    public void cancelTransfer(String reason) {
        // Verificar se a funcionalidade está habilitada
        boolean controlEnabled = Workflow.sideEffect(Boolean.class, () -> true);
        
        if (!controlEnabled) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Cancel functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        
        this.cancelled = true;
        this.cancelReason = reason;
        this.lastControlAction = TransferControlAction.CANCEL;
        this.lastControlTimestamp = LocalDateTime.now();
        
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer cancelled via signal. Reason: {}. WorkflowId: {}", 
                      reason, Workflow.getInfo().getWorkflowId());
    }

    // Query Methods para status de controle
    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public TransferControlStatus getControlStatus() {
        return new TransferControlStatus()
                .setPaused(paused)
                .setCancelled(cancelled)
                .setPauseReason(pauseReason)
                .setCancelReason(cancelReason)
                .setLastControlAction(lastControlAction)
                .setLastControlTimestamp(lastControlTimestamp)
                .setWorkflowStatus(currentResponse != null ? currentResponse.getStatus().name() : "UNKNOWN");
    }
}
