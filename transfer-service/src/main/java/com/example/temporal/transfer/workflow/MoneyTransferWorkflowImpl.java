package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
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
import java.util.Random;

public class MoneyTransferWorkflowImpl implements MoneyTransferWorkflow {

    private TransferResponse currentResponse;

    private final ActivityOptions validationActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .setDoNotRetry(ValidationException.class.getName())
                    .build())
            .build();

    private final ActivityOptions accountActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofMillis(500))
                    .setMaximumInterval(Duration.ofSeconds(5))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(5)
                    .build())
            .build();

    private final ActivityOptions notificationActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(10)
                    .build())
            .build();

    private final MoneyTransferActivities validationActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, validationActivityOptions);

    private final MoneyTransferActivities accountActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, accountActivityOptions);

    private final MoneyTransferActivities notificationActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, notificationActivityOptions);

    @Override
    public TransferResponse executeTransfer(TransferRequest transferRequest) {
        // Create Saga for compensation
        Saga saga = new Saga(
                new Saga.Options.Builder()
                        .setParallelCompensation(false)
                        .build()
        );
        
        currentResponse = new TransferResponse()
                .setTransferId(new Random().nextLong())
                .setSourceAccountNumber(transferRequest.getSourceAccountNumber())
                .setDestinationAccountNumber(transferRequest.getDestinationAccountNumber())
                .setAmount(transferRequest.getAmount())
                .setCurrency(transferRequest.getCurrency())
                .setStatus(TransferStatus.INITIATED);

        try {
            // Set initial status
            currentResponse.setStatus(TransferStatus.INITIATED);
            notificationActivities.notifyTransferInitiated(currentResponse.getTransferId());

            // Validate transfer with dedicated retry policy
            try {
                validationActivities.validateTransfer(transferRequest);
                currentResponse.setStatus(TransferStatus.VALIDATED);
            } catch (ActivityFailure e) {
                currentResponse.setStatus(TransferStatus.FAILED);
                notificationActivities.notifyTransferFailed(currentResponse.getTransferId(), e.getMessage());
                throw e;
            }

            // Lock accounts with specific retry policy
            accountActivities.lockAccounts(
                transferRequest.getSourceAccountNumber(),
                transferRequest.getDestinationAccountNumber()
            );
            saga.addCompensation(accountActivities::unlockAccounts,
                transferRequest.getSourceAccountNumber(),
                transferRequest.getDestinationAccountNumber());

            // Debit source account with enhanced monitoring
            accountActivities.debitAccount(
                transferRequest.getSourceAccountNumber(),
                transferRequest.getAmount()
            );
            saga.addCompensation(accountActivities::compensateDebit,
                transferRequest.getSourceAccountNumber(),
                transferRequest.getAmount());

            // Credit destination account with error handling
            try {
                accountActivities.creditAccount(
                    transferRequest.getDestinationAccountNumber(),
                    transferRequest.getAmount()
                );
                saga.addCompensation(accountActivities::compensateCredit,
                    transferRequest.getDestinationAccountNumber(),
                    transferRequest.getAmount());

            } catch (ActivityFailure e) {
                currentResponse.setStatus(TransferStatus.COMPENSATING);
                currentResponse.setFailureReason(e.getMessage());
                
                // Compensate all activities in reverse order
                saga.compensate();
                
                currentResponse.setStatus(TransferStatus.COMPENSATED);
                notificationActivities.notifyTransferFailed(currentResponse.getTransferId(), e.getMessage());
                
                throw e;
            }

            // Complete the transfer with notifications
            currentResponse.setStatus(TransferStatus.COMPLETED);
            notificationActivities.notifyTransferCompleted(currentResponse.getTransferId());
        } catch (ActivityFailure e) {
            // If we haven't handled the error already, do so now
            if (currentResponse.getStatus() != TransferStatus.FAILED && 
                currentResponse.getStatus() != TransferStatus.COMPENSATED) {
                currentResponse.setStatus(TransferStatus.FAILED);
                notificationActivities.notifyTransferFailed(currentResponse.getTransferId(), e.getMessage());
            }
            throw e;
        }

        return currentResponse;
    }
    @Override
    public TransferResponse getStatus() {
        return currentResponse;
    }
}
