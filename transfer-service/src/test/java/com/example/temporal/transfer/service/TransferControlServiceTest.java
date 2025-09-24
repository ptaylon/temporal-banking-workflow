package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferControlResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.common.dto.TransferControlAction;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes para funcionalidades de controle do TransferControlService
 */
@ExtendWith(MockitoExtension.class)
public class TransferControlServiceTest {

    @Mock
    private WorkflowClient workflowClient;

    @Mock
    private MoneyTransferWorkflow workflowStub;
    private TransferControlService transferControlService;

    @BeforeEach
    void setUp() {
        transferControlService = new TransferControlService(workflowClient);
    }
    
    private TransferControlService createTestService() {
        return new TransferControlService(workflowClient) {
            @Override
            protected boolean isWorkflowActive(String workflowId) {
                return true; // Always return active for tests
            }
        };
    }

    @Test
    void testPauseTransferSuccess() {
        // Arrange
        String workflowId = "transfer-123";
        TransferControlStatus mockStatus = createMockControlStatus(true, false, TransferControlAction.PAUSE);
        
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenReturn(mockStatus);

        // Act
        TransferControlResponse response = testService.pauseTransfer(workflowId);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertEquals("Transfer paused successfully", response.getMessage());
        assertNotNull(response.getStatus());
        assertTrue(response.getStatus().isPaused());

        verify(workflowStub).pauseTransfer();
        verify(workflowStub).getControlStatus();
    }

    @Test
    void testPauseTransferFailure() {
        // Arrange
        String workflowId = "transfer-123";
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        doThrow(new RuntimeException("Workflow not found")).when(workflowStub).pauseTransfer();

        // Act
        TransferControlResponse response = testService.pauseTransfer(workflowId);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertTrue(response.getMessage().contains("Failed to pause transfer"));

        verify(workflowStub).pauseTransfer();
        verify(workflowStub, never()).getControlStatus();
    }

    @Test
    void testResumeTransferSuccess() {
        // Arrange
        String workflowId = "transfer-456";
        TransferControlStatus mockStatus = createMockControlStatus(false, false, TransferControlAction.RESUME);
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenReturn(mockStatus);

        // Act
        TransferControlResponse response = testService.resumeTransfer(workflowId);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertEquals("Transfer resumed successfully", response.getMessage());
        assertNotNull(response.getStatus());
        assertFalse(response.getStatus().isPaused());

        verify(workflowStub).resumeTransfer();
        verify(workflowStub).getControlStatus();
    }

    @Test
    void testResumeTransferFailure() {
        // Arrange
        String workflowId = "transfer-456";
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        doThrow(new RuntimeException("Workflow not running")).when(workflowStub).resumeTransfer();

        // Act
        TransferControlResponse response = testService.resumeTransfer(workflowId);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertTrue(response.getMessage().contains("Failed to resume transfer"));

        verify(workflowStub).resumeTransfer();
        verify(workflowStub, never()).getControlStatus();
    }

    @Test
    void testCancelTransferSuccess() {
        // Arrange
        String workflowId = "transfer-789";
        String reason = "User requested cancellation";
        TransferControlStatus mockStatus = createMockControlStatus(false, true, TransferControlAction.CANCEL);
        mockStatus.setCancelReason(reason);
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenReturn(mockStatus);

        // Act
        TransferControlResponse response = testService.cancelTransfer(workflowId, reason);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertEquals("Transfer cancelled successfully", response.getMessage());
        assertNotNull(response.getStatus());
        assertTrue(response.getStatus().isCancelled());
        assertEquals(reason, response.getStatus().getCancelReason());

        verify(workflowStub).cancelTransfer(reason);
        verify(workflowStub).getControlStatus();
    }

    @Test
    void testCancelTransferWithNullReason() {
        // Arrange
        String workflowId = "transfer-789";
        TransferControlStatus mockStatus = createMockControlStatus(false, true, TransferControlAction.CANCEL);
        mockStatus.setCancelReason("Cancelled by user");
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenReturn(mockStatus);

        // Act
        TransferControlResponse response = testService.cancelTransfer(workflowId, null);

        // Assert
        assertTrue(response.isSuccess());
        
        verify(workflowStub).cancelTransfer("Cancelled by user");
        verify(workflowStub).getControlStatus();
    }

    @Test
    void testCancelTransferFailure() {
        // Arrange
        String workflowId = "transfer-789";
        String reason = "System error";
        TransferControlService testService = createTestService();
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        doThrow(new RuntimeException("Cannot cancel completed workflow")).when(workflowStub).cancelTransfer(reason);

        // Act
        TransferControlResponse response = testService.cancelTransfer(workflowId, reason);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(workflowId, response.getWorkflowId());
        assertTrue(response.getMessage().contains("Failed to cancel transfer"));

        verify(workflowStub).cancelTransfer(reason);
        verify(workflowStub, never()).getControlStatus();
    }

    @Test
    void testGetControlStatusSuccess() {
        // Arrange
        String workflowId = "transfer-999";
        TransferControlStatus mockStatus = createMockControlStatus(true, false, TransferControlAction.PAUSE);
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenReturn(mockStatus);

        // Act
        TransferControlStatus status = transferControlService.getControlStatus(workflowId);

        // Assert
        assertNotNull(status);
        assertTrue(status.isPaused());
        assertFalse(status.isCancelled());
        assertEquals(TransferControlAction.PAUSE, status.getLastControlAction());

        verify(workflowStub).getControlStatus();
    }

    @Test
    void testGetControlStatusFailure() {
        // Arrange
        String workflowId = "transfer-999";
        
        when(workflowClient.newWorkflowStub(MoneyTransferWorkflow.class, workflowId))
                .thenReturn(workflowStub);
        when(workflowStub.getControlStatus()).thenThrow(new RuntimeException("Workflow not found"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            transferControlService.getControlStatus(workflowId);
        });

        assertTrue(exception.getMessage().contains("Failed to get control status"));
        verify(workflowStub).getControlStatus();
    }

    private TransferControlStatus createMockControlStatus(boolean paused, boolean cancelled, TransferControlAction action) {
        TransferControlStatus status = new TransferControlStatus();
        status.setPaused(paused);
        status.setCancelled(cancelled);
        status.setLastControlAction(action);
        status.setLastControlTimestamp(LocalDateTime.now());
        status.setWorkflowStatus("RUNNING");
        return status;
    }
}