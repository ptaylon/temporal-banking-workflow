package com.example.temporal.transfer.domain.service;

import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.in.InitiateTransferUseCase;
import com.example.temporal.transfer.domain.port.out.TransferPersistencePort;
import com.example.temporal.transfer.domain.port.out.WorkflowOrchestrationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransferService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private TransferPersistencePort persistencePort;

    @Mock
    private WorkflowOrchestrationPort orchestrationPort;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(persistencePort, orchestrationPort);
    }

    @Test
    @DisplayName("Should initiate valid transfer")
    void shouldInitiateValidTransfer() {
        // Arrange
        var command = createValidCommand();
        
        when(persistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        
        var savedTransfer = TransferDomain.builder()
                .id(1L)
                .status(com.example.temporal.common.model.TransferStatus.INITIATED)
                .sourceAccountNumber("123456")
                .destinationAccountNumber("789012")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .build();
        when(persistencePort.save(any())).thenReturn(savedTransfer);

        // Act
        var result = transferService.initiateTransfer(command);

        // Assert
        assertTrue(result.getStatus().equals("INITIATED"));
        assertEquals(1L, result.getTransferId());
        assertEquals("transfer-1", result.getWorkflowId());
        verify(persistencePort).save(any());
        verify(orchestrationPort).startTransferWorkflow(any(), any());
    }

    @Test
    @DisplayName("Should return existing transfer for duplicate request")
    void shouldReturnExistingTransferForDuplicate() {
        // Arrange
        var command = createValidCommand();

        var existingTransfer = TransferDomain.builder()
                .id(99L)
                .status(com.example.temporal.common.model.TransferStatus.INITIATED)
                .sourceAccountNumber("123456")
                .destinationAccountNumber("789012")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .build();
        when(persistencePort.findByIdempotencyKey("test-key")).thenReturn(Optional.of(existingTransfer));

        // Act
        var result = transferService.initiateTransfer(command);

        // Assert
        assertTrue(result.getStatus().equals("INITIATED"));
        assertEquals(99L, result.getTransferId());
        assertEquals("transfer-99", result.getWorkflowId());
        verify(persistencePort, never()).save(any());
        verify(orchestrationPort, never()).startTransferWorkflow(any(), any());
    }

    @Test
    @DisplayName("Should reject transfer when source and destination are same")
    void shouldRejectTransferWhenSameAccount() {
        // Arrange
        var command = InitiateTransferUseCase.InitiateTransferCommand.builder()
                .sourceAccountNumber("123456")
                .destinationAccountNumber("123456")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .build();

        // Act
        var result = transferService.initiateTransfer(command);

        // Assert
        assertTrue(result.getStatus().equals("ERROR"));
        assertTrue(result.getMessage().contains("cannot be the same"));
        verify(persistencePort, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transfer with invalid amount")
    void shouldRejectTransferWithInvalidAmount() {
        // Arrange
        var command = InitiateTransferUseCase.InitiateTransferCommand.builder()
                .sourceAccountNumber("123456")
                .destinationAccountNumber("789012")
                .amount(new BigDecimal("-100.00"))
                .currency("BRL")
                .build();

        // Act
        var result = transferService.initiateTransfer(command);

        // Assert
        assertTrue(result.getStatus().equals("ERROR"));
        assertTrue(result.getMessage().contains("must be positive"));
    }

    @Test
    @DisplayName("Should get transfer by ID")
    void shouldGetTransferById() {
        // Arrange
        var transfer = TransferDomain.builder().id(1L).build();
        when(persistencePort.findById(1L)).thenReturn(Optional.of(transfer));

        // Act
        var result = transferService.getTransferById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Should get transfer by workflow ID")
    void shouldGetTransferByWorkflowId() {
        // Arrange
        var transfer = TransferDomain.builder().id(1L).build();
        when(persistencePort.findById(1L)).thenReturn(Optional.of(transfer));

        // Act
        var result = transferService.getTransferByWorkflowId("transfer-1");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Should return empty for invalid workflow ID format")
    void shouldReturnEmptyForInvalidWorkflowId() {
        // Act
        var result = transferService.getTransferByWorkflowId("invalid-format");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get transfers by account number")
    void shouldGetTransfersByAccountNumber() {
        // Arrange
        var transfers = java.util.List.of(
                TransferDomain.builder().id(1L).build(),
                TransferDomain.builder().id(2L).build()
        );
        when(persistencePort.findByAccountNumber("123456")).thenReturn(transfers);

        // Act
        var result = transferService.getTransfersByAccount("123456");

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should get transfer status")
    void shouldGetTransferStatus() {
        // Arrange
        var transfer = TransferDomain.builder()
                .id(1L)
                .status(com.example.temporal.common.model.TransferStatus.INITIATED)
                .build();
        when(persistencePort.findById(1L)).thenReturn(Optional.of(transfer));
        when(orchestrationPort.isWorkflowRunning("transfer-1")).thenReturn(false);

        // Act
        var result = transferService.getTransferStatus("transfer-1");

        // Assert
        assertNotNull(result);
        assertEquals("INITIATED", result.getWorkflowStatus());
        assertFalse(result.isWorkflowRunning());
    }

    private InitiateTransferUseCase.InitiateTransferCommand createValidCommand() {
        return InitiateTransferUseCase.InitiateTransferCommand.builder()
                .sourceAccountNumber("123456")
                .destinationAccountNumber("789012")
                .amount(new BigDecimal("100.00"))
                .currency("BRL")
                .idempotencyKey("test-key")
                .build();
    }
}
