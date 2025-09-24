package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.common.dto.TransferControlAction;
import com.example.temporal.common.workflow.MoneyTransferActivities;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para funcionalidades de controle (Signals e Queries) do MoneyTransferWorkflow
 * Usando abordagem simplificada focada apenas nos métodos de controle
 */
public class MoneyTransferWorkflowControlTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflow = TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(MoneyTransferWorkflowImpl.class)
            .setDoNotStart(true)
            .build();

    @Test
    public void testWorkflowControlSignalsAndQueries(TestWorkflowEnvironment testEnv, Worker worker,
                                                   MoneyTransferWorkflow workflow) {
        // Setup test activities implementation
        MoneyTransferActivities activities = new TestMoneyTransferActivitiesImpl();
        worker.registerActivitiesImplementations(activities);
        testEnv.start();

        // Criar um workflow stub com ID específico usando WorkflowClient
        WorkflowClient client = testEnv.getWorkflowClient();
        String workflowId = "test-control-" + System.currentTimeMillis();
        
        MoneyTransferWorkflow workflowStub = client.newWorkflowStub(
            MoneyTransferWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build()
        );

        TransferRequest request = createTestRequest();

        // Iniciar o workflow usando WorkflowClient.start() para obter um stub válido
        WorkflowClient.start(workflowStub::executeTransfer, request);

        // Aguardar um pouco para o workflow iniciar
        testEnv.sleep(java.time.Duration.ofMillis(100));

        // Testar estado inicial
        assertFalse(workflowStub.isPaused());
        
        TransferControlStatus initialStatus = workflowStub.getControlStatus();
        assertFalse(initialStatus.isPaused());
        assertFalse(initialStatus.isCancelled());
        assertNull(initialStatus.getLastControlAction());

        // Testar pause
        workflowStub.pauseTransfer();
        assertTrue(workflowStub.isPaused());
        
        TransferControlStatus pausedStatus = workflowStub.getControlStatus();
        assertTrue(pausedStatus.isPaused());
        assertFalse(pausedStatus.isCancelled());
        assertEquals(TransferControlAction.PAUSE, pausedStatus.getLastControlAction());
        assertNotNull(pausedStatus.getLastControlTimestamp());

        // Testar resume
        workflowStub.resumeTransfer();
        assertFalse(workflowStub.isPaused());
        
        TransferControlStatus resumedStatus = workflowStub.getControlStatus();
        assertFalse(resumedStatus.isPaused());
        assertFalse(resumedStatus.isCancelled());
        assertEquals(TransferControlAction.RESUME, resumedStatus.getLastControlAction());

        // Testar cancel
        workflowStub.cancelTransfer("Test cancellation");
        
        TransferControlStatus cancelledStatus = workflowStub.getControlStatus();
        assertFalse(cancelledStatus.isPaused());
        assertTrue(cancelledStatus.isCancelled());
        assertEquals("Test cancellation", cancelledStatus.getCancelReason());
        assertEquals(TransferControlAction.CANCEL, cancelledStatus.getLastControlAction());
    }

    @Test
    public void testMultipleControlActions(TestWorkflowEnvironment testEnv, Worker worker,
                                         MoneyTransferWorkflow workflow) {
        // Setup test activities implementation
        MoneyTransferActivities activities = new TestMoneyTransferActivitiesImpl();
        worker.registerActivitiesImplementations(activities);
        testEnv.start();

        // Criar um workflow stub com ID específico
        WorkflowClient client = testEnv.getWorkflowClient();
        String workflowId = "test-multiple-" + System.currentTimeMillis();
        
        MoneyTransferWorkflow workflowStub = client.newWorkflowStub(
            MoneyTransferWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build()
        );

        TransferRequest request = createTestRequest();

        // Iniciar o workflow
        WorkflowClient.start(workflowStub::executeTransfer, request);

        testEnv.sleep(java.time.Duration.ofMillis(100));

        // Múltiplas chamadas de pause
        workflowStub.pauseTransfer();
        assertTrue(workflowStub.isPaused());
        
        workflowStub.pauseTransfer(); // Deve continuar pausado
        assertTrue(workflowStub.isPaused());

        // Múltiplas chamadas de resume
        workflowStub.resumeTransfer();
        assertFalse(workflowStub.isPaused());
        
        workflowStub.resumeTransfer(); // Deve continuar não pausado
        assertFalse(workflowStub.isPaused());

        // Cancelamento final
        workflowStub.cancelTransfer("Final cancellation");
        
        TransferControlStatus status = workflowStub.getControlStatus();
        assertTrue(status.isCancelled());
        assertEquals("Final cancellation", status.getCancelReason());
    }

    private TransferRequest createTestRequest() {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountNumber("123");
        request.setDestinationAccountNumber("456");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        return request;
    }

    /**
     * Implementação de teste das activities que simula uma operação lenta
     */
    private static class TestMoneyTransferActivitiesImpl implements MoneyTransferActivities {
        
        @Override
        public void validateTransfer(TransferRequest request) {
            // Simular uma operação lenta para dar tempo aos signals
            try {
                Thread.sleep(2000); // 2 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        @Override
        public void lockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
            // Implementação vazia para teste
        }
        
        @Override
        public void debitAccount(String accountNumber, BigDecimal amount) {
            // Implementação vazia para teste
        }
        
        @Override
        public void creditAccount(String accountNumber, BigDecimal amount) {
            // Implementação vazia para teste
        }
        
        @Override
        public void unlockAccounts(String sourceAccountNumber, String destinationAccountNumber) {
            // Implementação vazia para teste
        }
        
        @Override
        public void compensateDebit(String accountNumber, BigDecimal amount) {
            // Implementação vazia para teste
        }
        
        @Override
        public void compensateCredit(String accountNumber, BigDecimal amount) {
            // Implementação vazia para teste
        }
        
        @Override
        public void notifyTransferInitiated(Long transferId) {
            // Implementação vazia para teste
        }
        
        @Override
        public void notifyTransferCompleted(Long transferId) {
            // Implementação vazia para teste
        }
        
        @Override
        public void notifyTransferFailed(Long transferId, String reason) {
            // Implementação vazia para teste
        }
        
        @Override
        public void updateTransferStatus(Long transferId, String status) {
            // Implementação vazia para teste
        }
        
        @Override
        public void updateTransferStatusWithReason(Long transferId, String status, String reason) {
            // Implementação vazia para teste
        }
    }
}