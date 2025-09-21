package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class MoneyTransferWorkflowTest {

    private MoneyTransferActivities activities;

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflow = TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(MoneyTransferWorkflowImpl.class)
            .setDoNotStart(true)
            .build();

    public void setUp(TestWorkflowEnvironment testEnv, Worker worker) {
        // Create a mock for activities
        activities = Mockito.mock(MoneyTransferActivities.class);
        
        // Register mock activities implementation
        worker.registerActivitiesImplementations(new MoneyTransferActivitiesImpl(activities));
        testEnv.start();
    }
    
    private static class MoneyTransferActivitiesImpl implements MoneyTransferActivities {
        private final MoneyTransferActivities delegate;
        
        public MoneyTransferActivitiesImpl(MoneyTransferActivities delegate) {
            this.delegate = delegate;
        }
        
        public void validateTransfer(TransferRequest request) {
            delegate.validateTransfer(request);
        }
        
        public void lockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
            delegate.lockAccounts(sourceAccountNumber, destinationAccountNumber);
        }
        
        public void debitAccount(String accountNumber, BigDecimal amount) {
            delegate.debitAccount(accountNumber, amount);
        }
        
        public void creditAccount(String accountNumber, BigDecimal amount) {
            delegate.creditAccount(accountNumber, amount);
        }
        
        public void unlockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
            delegate.unlockAccounts(sourceAccountNumber, destinationAccountNumber);
        }
        
        public void compensateDebit(String accountNumber, BigDecimal amount) {
            delegate.compensateDebit(accountNumber, amount);
        }
        
        public void compensateCredit(String accountNumber, BigDecimal amount) {
            delegate.compensateCredit(accountNumber, amount);
        }
        
        public void notifyTransferInitiated(Long transferId) {
            delegate.notifyTransferInitiated(transferId);
        }
        
        public void notifyTransferCompleted(Long transferId) {
            delegate.notifyTransferCompleted(transferId);
        }
        
        public void notifyTransferFailed(Long transferId, String reason) {
            delegate.notifyTransferFailed(transferId, reason);
        }
    }

@Test
    public void testSuccessfulTransfer(TestWorkflowEnvironment testEnv, Worker worker,
                                     MoneyTransferWorkflow workflow) throws ExecutionException, InterruptedException {
        setUp(testEnv, worker);

        // Create test request
        TransferRequest request = new TransferRequest();
        request.setSourceAccountNumber("123");
        request.setDestinationAccountNumber("456");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        TransferResponse response = workflow.executeTransfer(request);

        // Verify response
        assertNotNull(response);
        assertEquals(TransferStatus.COMPLETED, response.getStatus());
        assertEquals(request.getSourceAccountNumber(), response.getSourceAccountNumber());
        assertEquals(request.getDestinationAccountNumber(), response.getDestinationAccountNumber());
        assertEquals(request.getAmount(), response.getAmount());
        assertEquals(request.getCurrency(), response.getCurrency());

        // Verify activity executions in correct order
        var inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).notifyTransferInitiated(any());
        inOrder.verify(activities).validateTransfer(request);
        inOrder.verify(activities).lockAccounts(request.getSourceAccountNumber(), request.getDestinationAccountNumber());
        inOrder.verify(activities).debitAccount(request.getSourceAccountNumber(), request.getAmount());
        inOrder.verify(activities).creditAccount(request.getDestinationAccountNumber(), request.getAmount());
        inOrder.verify(activities).notifyTransferCompleted(any());
        inOrder.verifyNoMoreInteractions();
    }

@Test
    public void testFailedValidation(TestWorkflowEnvironment testEnv, Worker worker,
                                   MoneyTransferWorkflow workflow) throws ExecutionException, InterruptedException {
        setUp(testEnv, worker);

        // Create test request
        TransferRequest request = new TransferRequest();
        request.setSourceAccountNumber("123");
        request.setDestinationAccountNumber("456");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        // Mock validation failure
        doThrow(new RuntimeException("Insufficient funds"))
                .when(activities).validateTransfer(request);

        try {
            workflow.executeTransfer(request);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Verify activity executions in correct order
        var inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).notifyTransferInitiated(any());
        inOrder.verify(activities, atLeastOnce()).validateTransfer(request);
        inOrder.verify(activities).notifyTransferFailed(any(), anyString());
        inOrder.verifyNoMoreInteractions();

        // Verify that account activities were never called
        verify(activities, never()).lockAccounts(anyString(), anyString());
        verify(activities, never()).debitAccount(anyString(), any());
        verify(activities, never()).creditAccount(anyString(), any());
    }

@Test
    public void testCompensation(TestWorkflowEnvironment testEnv, Worker worker,
                               MoneyTransferWorkflow workflow) throws ExecutionException, InterruptedException {
        setUp(testEnv, worker);

        // Create test request
        TransferRequest request = new TransferRequest();
        request.setSourceAccountNumber("123");
        request.setDestinationAccountNumber("456");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        // Mock credit failure to trigger compensation
        doThrow(new RuntimeException("Credit failed"))
                .when(activities).creditAccount(request.getDestinationAccountNumber(), request.getAmount());

        try {
            workflow.executeTransfer(request);
        } catch (RuntimeException e) {
            // Expected exception
        }

        // Verify activity executions in correct order
        var inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).notifyTransferInitiated(any());
        inOrder.verify(activities).validateTransfer(request);
        inOrder.verify(activities).lockAccounts(request.getSourceAccountNumber(), request.getDestinationAccountNumber());
        inOrder.verify(activities).debitAccount(request.getSourceAccountNumber(), request.getAmount());
        inOrder.verify(activities, atLeastOnce()).creditAccount(request.getDestinationAccountNumber(), request.getAmount());
        inOrder.verify(activities).compensateDebit(request.getSourceAccountNumber(), request.getAmount());
        inOrder.verify(activities).unlockAccounts(request.getSourceAccountNumber(), request.getDestinationAccountNumber());
        inOrder.verify(activities).notifyTransferFailed(any(), anyString());
        inOrder.verifyNoMoreInteractions();
    }
}