package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.Random;

/**
 * Money Transfer Workflow implementation using Temporal.
 * <p>
 * Orchestrates the transfer process with:
 * - Configurable delays between steps
 * - Pause/Resume/Cancel support via signals
 * - Saga pattern for compensation on failure
 * - Search attributes for visibility
 * </p>
 */
public class MoneyTransferWorkflowImpl implements MoneyTransferWorkflow {

    // ========== Configuration Constants ==========
    private static final Duration DEFAULT_STEP_DELAY = Duration.ofSeconds(20);

    // ========== Workflow State ==========
    private TransferResponse currentResponse;
    private final TransferControlState controlState;
    private final SearchAttributesManager searchAttributesManager;

    // ========== Cancellation Scopes ==========
    private CancellationScope mainScope;
    private CancellationScope delayScope;

    // ========== Activity Stubs ==========
    private final MoneyTransferActivities validationActivities;
    private final MoneyTransferActivities accountActivities;
    private final MoneyTransferActivities notificationActivities;
    private final MoneyTransferActivities persistenceActivities;

    /**
     * Constructs the workflow with all dependencies initialized.
     */
    public MoneyTransferWorkflowImpl() {
        this.controlState = new TransferControlState();
        this.searchAttributesManager = new SearchAttributesManager(Workflow.getInfo().getNamespace());

        // Initialize activity stubs with specific configurations
        this.validationActivities = Workflow.newActivityStub(
                MoneyTransferActivities.class,
                ActivityConfiguration.createValidationOptions());

        this.accountActivities = Workflow.newActivityStub(
                MoneyTransferActivities.class,
                ActivityConfiguration.createAccountOptions());

        this.notificationActivities = Workflow.newActivityStub(
                MoneyTransferActivities.class,
                ActivityConfiguration.createNotificationOptions());

        this.persistenceActivities = Workflow.newActivityStub(
                MoneyTransferActivities.class,
                ActivityConfiguration.createPersistenceOptions());
    }

    // ========== Main Workflow Entry Point ==========

    @Override
    public TransferResponse executeTransfer(final TransferRequest request) {
        final Long transferId = generateTransferId(request);
        final Saga saga = createSaga();

        initializeWorkflow(request, transferId);

        // Handle pre-execution delay if configured
        if (hasConfigurableDelay(request)) {
            executeConfigurableDelay(request, transferId);
        }

        // Check if cancelled during delay
        if (controlState.isCancelled()) {
            return cancelDuringDelay(transferId);
        }

        // Execute main transfer logic with cancellation support
        return executeWithCancellationSupport(request, saga, transferId);
    }

    // ========== Workflow Initialization ==========

    /**
     * Initializes workflow state and search attributes.
     */
    private void initializeWorkflow(final TransferRequest request, final Long transferId) {
        currentResponse = buildInitialResponse(request, transferId);
        searchAttributesManager.upsertInitialAttributes(request, transferId);
    }

    /**
     * Builds initial transfer response.
     */
    private TransferResponse buildInitialResponse(final TransferRequest request, final Long transferId) {
        return new TransferResponse()
                .setTransferId(transferId)
                .setSourceAccountNumber(request.getSourceAccountNumber())
                .setDestinationAccountNumber(request.getDestinationAccountNumber())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setStatus(TransferStatus.INITIATED);
    }

    /**
     * Checks if request has configurable delay.
     */
    private boolean hasConfigurableDelay(final TransferRequest request) {
        return request.getDelayInSeconds() != null && request.getDelayInSeconds() > 0;
    }

    // ========== Delay Handling ==========

    /**
     * Executes configurable delay with cancellation support.
     */
    private void executeConfigurableDelay(final TransferRequest request, final Long transferId) {
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Starting configurable delay: {} seconds for transfer {}",
                        request.getDelayInSeconds(), transferId);

        final Duration delay = Duration.ofSeconds(request.getDelayInSeconds());

        delayScope = Workflow.newCancellationScope(() -> {
            executeDelayLogic(request, delay, transferId);
        });

        delayScope.run();
    }

    /**
     * Internal delay logic with cancellation check.
     */
    private void executeDelayLogic(final TransferRequest request, final Duration delay, final Long transferId) {
        try {
            if (request.isAllowCancelDuringDelay()) {
                waitForDelayOrCancellation(delay, transferId);
            } else {
                Workflow.sleep(delay);
            }

            if (!controlState.isDelayCancelled()) {
                controlState.markDelayCompleted();
            }

        } catch (final Exception e) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Delay interrupted for transfer {}: {}", transferId, e.getMessage());
            controlState.markDelayCancelled();
        }
    }

    /**
     * Waits for either delay completion or cancellation signal.
     */
    private void waitForDelayOrCancellation(final Duration delay, final Long transferId) {
        final Promise<Void> timerPromise = Workflow.newTimer(delay);
        Promise.anyOf(timerPromise, Workflow.newPromise(controlState.isCancelled())).get();

        if (controlState.isCancelled()) {
            controlState.markDelayCancelled();
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer {} cancelled during delay period", transferId);
        } else {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Delay completed for transfer {}", transferId);
        }
    }

    /**
     * Handles cancellation during delay period.
     */
    private TransferResponse cancelDuringDelay(final Long transferId) {
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer cancelled during delay period. WorkflowId: {}",
                        Workflow.getInfo().getWorkflowId());

        currentResponse.setStatus(TransferStatus.CANCELLED);
        return currentResponse;
    }

    // ========== Main Execution with Cancellation ==========

    /**
     * Executes transfer with cancellation scope support.
     */
    private TransferResponse executeWithCancellationSupport(
            final TransferRequest request,
            final Saga saga,
            final Long transferId) {

        mainScope = Workflow.newCancellationScope(() -> {
            try {
                executeTransferSteps(request, saga, transferId);
            } catch (final ActivityFailure e) {
                handleTransferFailure(transferId, e);
                throw e;
            }
        });

        try {
            mainScope.run();
            return currentResponse;

        } catch (final io.temporal.failure.CanceledFailure e) {
            return handleWorkflowCancellation(saga, transferId, e);
        }
    }

    /**
     * Handles workflow cancellation with saga compensation.
     */
    private TransferResponse handleWorkflowCancellation(
            final Saga saga,
            final Long transferId,
            final io.temporal.failure.CanceledFailure e) {

        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer workflow cancelled: {}. Executing rollback via Saga. WorkflowId: {}",
                        e.getMessage(), Workflow.getInfo().getWorkflowId());

        // Execute compensation in detached scope
        executeCompensation(saga, transferId);

        currentResponse.setStatus(TransferStatus.CANCELLED);
        return currentResponse;
    }

    /**
     * Executes saga compensation in detached scope.
     */
    private void executeCompensation(final Saga saga, final Long transferId) {
        final CancellationScope nonCancellable = Workflow.newDetachedCancellationScope(() -> {
            try {
                currentResponse.setStatus(TransferStatus.COMPENSATING);
                saga.compensate();

                Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                        .info("Saga rollback completed successfully. Transfer cancelled cleanly. WorkflowId: {}",
                                Workflow.getInfo().getWorkflowId());

            } catch (final Exception sagaException) {
                Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                        .error("Error during Saga rollback: {}. WorkflowId: {}",
                                sagaException.getMessage(), Workflow.getInfo().getWorkflowId());
            }
        });

        nonCancellable.run();
    }

    // ========== Transfer Steps Execution ==========

    /**
     * Executes all transfer steps with pause support.
     */
    private void executeTransferSteps(final TransferRequest request, final Saga saga, final Long transferId) {
        // Step 1: Initialize
        executeStepWithPauseCheck(() -> {
            sleepIfConfigured(DEFAULT_STEP_DELAY);
            initializeTransfer(transferId);
        });

        // Step 2: Validate
        executeStepWithPauseCheck(() -> {
            sleepIfConfigured(DEFAULT_STEP_DELAY);
            validateTransfer(request, transferId);
        });

        // Step 3: Account Operations
        executeStepWithPauseCheck(() -> {
            sleepIfConfigured(DEFAULT_STEP_DELAY);
            executeAccountOperations(request, saga, transferId);
        });

        // Step 4: Complete
        executeStepWithPauseCheck(() -> {
            sleepIfConfigured(DEFAULT_STEP_DELAY);
            completeTransfer(transferId);
        });
    }

    /**
     * Executes a step after waiting for pause condition.
     */
    private void executeStepWithPauseCheck(final Runnable step) {
        waitForResume();
        step.run();
    }

    /**
     * Waits until workflow is not paused.
     */
    private void waitForResume() {
        if (controlState.isPaused()) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer paused, waiting for resume signal. WorkflowId: {}",
                            Workflow.getInfo().getWorkflowId());
        }
        Workflow.await(() -> !controlState.isPaused());
    }

    /**
     * Applies sleep if delay is configured.
     */
    private void sleepIfConfigured(final Duration delay) {
        if (delay != null && !delay.isZero() && !delay.isNegative()) {
            Workflow.sleep(delay);
        }
    }

    // ========== Individual Step Implementations ==========

    /**
     * Step 1: Initialize transfer.
     */
    private void initializeTransfer(final Long transferId) {
        currentResponse.setStatus(TransferStatus.INITIATED);
        searchAttributesManager.updateStatusAttribute(TransferStatus.INITIATED);
        persistenceActivities.updateTransferStatus(transferId, TransferStatus.INITIATED);
        notificationActivities.notifyTransferInitiated(transferId);
    }

    /**
     * Step 2: Validate transfer request.
     */
    private void validateTransfer(final TransferRequest request, final Long transferId) {
        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Starting validation for transfer ID: {} - Will retry up to 20 times for connectivity issues",
                        transferId);

        validationActivities.validateTransfer(request);

        currentResponse.setStatus(TransferStatus.VALIDATED);
        searchAttributesManager.updateStatusAttribute(TransferStatus.VALIDATED);
        persistenceActivities.updateTransferStatus(transferId, TransferStatus.VALIDATED);

        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Transfer validation successful for ID: {}", transferId);
    }

    /**
     * Step 3: Execute account operations (lock, debit, credit).
     */
    private void executeAccountOperations(final TransferRequest request, final Saga saga, final Long transferId) {
        lockAccountsWithCompensation(request, saga);
        debitAccountWithCompensation(request, saga);
        creditAccountWithCompensation(request, saga, transferId);
    }

    /**
     * Locks accounts and registers compensation.
     */
    private void lockAccountsWithCompensation(final TransferRequest request, final Saga saga) {
        accountActivities.lockAccounts(
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber());

        saga.addCompensation(
                accountActivities::unlockAccounts,
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber());
    }

    /**
     * Debits source account and registers compensation.
     */
    private void debitAccountWithCompensation(final TransferRequest request, final Saga saga) {
        accountActivities.debitAccount(
                request.getSourceAccountNumber(),
                request.getAmount());

        saga.addCompensation(
                accountActivities::compensateDebit,
                request.getSourceAccountNumber(),
                request.getAmount());
    }

    /**
     * Credits destination account with error handling.
     */
    private void creditAccountWithCompensation(
            final TransferRequest request,
            final Saga saga,
            final Long transferId) {

        try {
            accountActivities.creditAccount(
                    request.getDestinationAccountNumber(),
                    request.getAmount());

            saga.addCompensation(
                    accountActivities::compensateCredit,
                    request.getDestinationAccountNumber(),
                    request.getAmount());

        } catch (final ActivityFailure e) {
            handleCreditFailureWithCompensation(saga, transferId, e);
            throw e;
        }
    }

    /**
     * Handles credit failure with compensation.
     */
    private void handleCreditFailureWithCompensation(
            final Saga saga,
            final Long transferId,
            final ActivityFailure e) {

        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .warn("Credit account failed for transfer ID: {}, starting compensation. Error: {}",
                        transferId, e.getMessage());

        currentResponse.setStatus(TransferStatus.COMPENSATING);
        currentResponse.setFailureReason(e.getMessage());
        persistenceActivities.updateTransferStatusWithReason(
                transferId, TransferStatus.COMPENSATING, e.getMessage());

        saga.compensate();

        currentResponse.setStatus(TransferStatus.COMPENSATED);
        persistenceActivities.updateTransferStatusWithReason(
                transferId, TransferStatus.COMPENSATED, e.getMessage());

        notificationActivities.notifyTransferFailed(transferId, e.getMessage());

        Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                .info("Compensation completed for transfer ID: {}", transferId);
    }

    /**
     * Step 4: Complete transfer.
     */
    private void completeTransfer(final Long transferId) {
        currentResponse.setStatus(TransferStatus.COMPLETED);
        searchAttributesManager.updateStatusAttribute(TransferStatus.COMPLETED);
        persistenceActivities.updateTransferStatus(transferId, TransferStatus.COMPLETED);
        notificationActivities.notifyTransferCompleted(transferId);
    }

    // ========== Failure Handling ==========

    /**
     * Handles transfer failure.
     */
    private void handleTransferFailure(final Long transferId, final ActivityFailure e) {
        currentResponse.setStatus(TransferStatus.FAILED);
        final String truncatedError = truncateErrorMessage(e.getMessage());
        persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.FAILED, truncatedError);
        notificationActivities.notifyTransferFailed(transferId, truncatedError);
    }

    /**
     * Truncates error messages to prevent database overflow.
     */
    private String truncateErrorMessage(final String errorMessage) {
        if (errorMessage == null) {
            return "";
        }
        return errorMessage.length() > 200 ? errorMessage.substring(0, 200) + "..." : errorMessage;
    }

    // ========== Utility Methods ==========

    /**
     * Generates transfer ID from request or creates random one.
     */
    private Long generateTransferId(final TransferRequest request) {
        return request.getTransferId() != null ? request.getTransferId() : new Random().nextLong();
    }

    /**
     * Creates Saga instance for compensation management.
     */
    private Saga createSaga() {
        return new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build());
    }

    // ========== Query Methods ==========

    @Override
    public TransferResponse getStatus() {
        return currentResponse;
    }

    @Override
    public boolean isPaused() {
        return controlState.isPaused();
    }

    @Override
    public com.example.temporal.common.dto.TransferControlStatus getControlStatus() {
        final var status = new com.example.temporal.common.dto.TransferControlStatus();
        status.setPaused(controlState.isPaused());
        status.setCancelled(controlState.isCancelled());
        status.setPauseReason(controlState.getPauseReason());
        status.setCancelReason(controlState.getCancelReason());
        status.setLastControlAction(controlState.getLastControlAction());
        status.setLastControlTimestamp(controlState.getLastControlTimestamp());
        status.setWorkflowStatus(currentResponse != null ? currentResponse.getStatus().name() : "UNKNOWN");
        return status;
    }

    // ========== Signal Methods ==========

    @Override
    public void pauseTransfer() {
        if (!isControlEnabled()) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Pause functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        controlState.pause(null);
    }

    @Override
    public void resumeTransfer() {
        if (!isControlEnabled()) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Resume functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        controlState.resume();
    }

    @Override
    public void cancelTransfer(final String reason) {
        if (!isControlEnabled()) {
            Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .warn("Cancel functionality is disabled. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
            return;
        }
        controlState.cancel(reason != null ? reason : "Cancelled by user request");
    }

    /**
     * Checks if control functionality is enabled.
     */
    private boolean isControlEnabled() {
        return Workflow.sideEffect(Boolean.class, () -> true);
    }
}