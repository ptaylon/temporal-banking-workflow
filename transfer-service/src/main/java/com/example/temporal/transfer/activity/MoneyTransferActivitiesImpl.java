package com.example.temporal.transfer.activity;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.transfer.client.AccountServiceClient;
import com.example.temporal.transfer.client.ValidationServiceClient;
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

    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    @Override
    public void validateTransfer(final TransferRequest request) {
        log.info("Validating transfer request: {}", request);
        validationServiceClient.validateTransfer(request);
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
    public void creditAccount(String accountNumber, BigDecimal amount) {
        log.info("Crediting account {} amount {}", accountNumber, amount);
        accountServiceClient.creditAccount(accountNumber, amount);
    }

    @Override
    public void unlockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
        log.info("Unlocking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);
        // In a real implementation, you would have an endpoint to unlock accounts
        // This is typically done automatically when the transaction is committed or rolled back
    }

    @Override
    public void compensateDebit(String accountNumber, BigDecimal amount) {
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
}