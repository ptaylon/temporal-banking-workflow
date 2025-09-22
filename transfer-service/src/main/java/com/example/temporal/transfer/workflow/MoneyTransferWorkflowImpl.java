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
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofMinutes(5)) // Até 5 minutos entre tentativas
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(20) // Muito mais tentativas para conectividade
                    .setDoNotRetry(ValidationException.class.getName()) // Apenas erros de negócio não devem ser retentados
                    .build())
            .build();

    private final ActivityOptions accountActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofMinutes(2))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(15) // Mais tentativas para operações bancárias críticas
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

    private final ActivityOptions persistenceActivityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(15) // Mais tentativas para operações de persistência
                    .build())
            .build();

    private final MoneyTransferActivities validationActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, validationActivityOptions);

    private final MoneyTransferActivities accountActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, accountActivityOptions);

    private final MoneyTransferActivities notificationActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, notificationActivityOptions);

    private final MoneyTransferActivities persistenceActivities =
            Workflow.newActivityStub(MoneyTransferActivities.class, persistenceActivityOptions);

    @Override
    public TransferResponse executeTransfer(TransferRequest transferRequest) {
        // Usar o ID do request ou gerar um aleatório se não fornecido
        Long transferId = transferRequest.getTransferId() != null ? 
            transferRequest.getTransferId() : new Random().nextLong();
        // Create Saga for compensation
        Saga saga = new Saga(
                new Saga.Options.Builder()
                        .setParallelCompensation(false)
                        .build()
        );
        
        currentResponse = new TransferResponse()
                .setTransferId(transferId)
                .setSourceAccountNumber(transferRequest.getSourceAccountNumber())
                .setDestinationAccountNumber(transferRequest.getDestinationAccountNumber())
                .setAmount(transferRequest.getAmount())
                .setCurrency(transferRequest.getCurrency())
                .setStatus(TransferStatus.INITIATED);

        try {
            // Set initial status
            currentResponse.setStatus(TransferStatus.INITIATED);
            persistenceActivities.updateTransferStatus(transferId, TransferStatus.INITIATED.name());
            notificationActivities.notifyTransferInitiated(currentResponse.getTransferId());

            // Validate transfer with dedicated retry policy and enhanced error handling
            try {
                Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Starting validation for transfer ID: {} - Attempt will retry up to 20 times for connectivity issues", transferId);
                
                validationActivities.validateTransfer(transferRequest);
                currentResponse.setStatus(TransferStatus.VALIDATED);
                persistenceActivities.updateTransferStatus(transferId, TransferStatus.VALIDATED.name());
                
                Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .info("Transfer validation successful for ID: {}", transferId);
                    
            } catch (ActivityFailure e) {
                Workflow.getLogger(MoneyTransferWorkflowImpl.class)
                    .error("Transfer validation failed permanently for ID: {} after all retries. Error: {}", transferId, e.getMessage());
                    
                currentResponse.setStatus(TransferStatus.FAILED);
                String truncatedError = e.getMessage().length() > 200 ? 
                    e.getMessage().substring(0, 200) + "..." : e.getMessage();
                persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.FAILED.name(), truncatedError);
                notificationActivities.notifyTransferFailed(currentResponse.getTransferId(), truncatedError);
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
                persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.COMPENSATING.name(), e.getMessage());
                
                // Compensate all activities in reverse order
                saga.compensate();
                
                currentResponse.setStatus(TransferStatus.COMPENSATED);
                persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.COMPENSATED.name(), e.getMessage());
                notificationActivities.notifyTransferFailed(currentResponse.getTransferId(), e.getMessage());
                
                throw e;
            }

            // Complete the transfer with notifications
            currentResponse.setStatus(TransferStatus.COMPLETED);
            persistenceActivities.updateTransferStatus(transferId, TransferStatus.COMPLETED.name());
            notificationActivities.notifyTransferCompleted(currentResponse.getTransferId());
        } catch (ActivityFailure e) {
            // If we haven't handled the error already, do so now
            if (currentResponse.getStatus() != TransferStatus.FAILED && 
                currentResponse.getStatus() != TransferStatus.COMPENSATED) {
                currentResponse.setStatus(TransferStatus.FAILED);
                persistenceActivities.updateTransferStatusWithReason(transferId, TransferStatus.FAILED.name(), e.getMessage());
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
