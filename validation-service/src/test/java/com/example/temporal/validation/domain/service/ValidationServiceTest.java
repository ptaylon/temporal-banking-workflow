package com.example.temporal.validation.domain.service;

import com.example.temporal.validation.domain.model.TransferValidationDomain;
import com.example.temporal.validation.domain.port.in.ValidateTransferUseCase;
import com.example.temporal.validation.domain.port.out.AccountServicePort;
import com.example.temporal.validation.domain.port.out.FraudRulePort;
import com.example.temporal.validation.domain.port.out.TransferLimitPort;
import com.example.temporal.validation.domain.port.out.ValidationPersistencePort;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService Unit Tests")
class ValidationServiceTest {

    @Mock private ValidationPersistencePort validationPersistencePort;
    @Mock private AccountServicePort accountServicePort;
    @Mock private TransferLimitPort transferLimitPort;
    @Mock private FraudRulePort fraudRulePort;

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService(
                validationPersistencePort, accountServicePort, transferLimitPort, fraudRulePort);
    }

    @Test
    @DisplayName("Should approve valid transfer")
    void shouldApproveValidTransfer() {
        var command = createValidCommand();
        mockAccountExists("123456", "BRL", new BigDecimal("1000.00"));
        mockAccountExists("789012", "BRL", new BigDecimal("500.00"));
        when(transferLimitPort.getByAccountTypeAndCurrency(any(), any())).thenReturn(Optional.empty());
        when(fraudRulePort.getActiveRules()).thenReturn(java.util.List.of());
        when(validationPersistencePort.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        var savedValidation = TransferValidationDomain.builder().id(1L)
                .validationResult(TransferValidationDomain.ValidationResult.APPROVED).build();
        when(validationPersistencePort.save(any())).thenReturn(savedValidation);

        var result = validationService.validateTransfer(command);

        assertTrue(result.approved());
        assertNotNull(result.validationId());
        verify(validationPersistencePort).save(any());
    }

    @Test
    @DisplayName("Should reject when source account does not exist")
    void shouldRejectWhenSourceAccountDoesNotExist() {
        var command = createValidCommand();
        when(accountServicePort.getAccount("123456")).thenReturn(Optional.empty());
        var result = validationService.validateTransfer(command);
        assertFalse(result.approved());
        assertTrue(result.rejectionReason().contains("does not exist"));
    }

    @Test
    @DisplayName("Should reject when insufficient funds")
    void shouldRejectWhenInsufficientFunds() {
        var command = createValidCommand();
        mockAccountExists("123456", "BRL", new BigDecimal("50.00"));
        mockAccountExists("789012", "BRL", new BigDecimal("500.00"));
        var result = validationService.validateTransfer(command);
        assertFalse(result.approved());
        assertTrue(result.rejectionReason().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Should return existing validation for duplicate request")
    void shouldReturnExistingValidationForDuplicate() {
        var command = createValidCommand();
        var existing = TransferValidationDomain.builder().id(99L)
                .validationResult(TransferValidationDomain.ValidationResult.APPROVED).fraudScore(10).build();
        when(validationPersistencePort.findByIdempotencyKey("test-key")).thenReturn(Optional.of(existing));
        var result = validationService.validateTransfer(command);
        assertTrue(result.approved());
        assertEquals(99L, result.validationId());
        verify(validationPersistencePort, never()).save(any());
    }

    private ValidateTransferUseCase.ValidateTransferCommand createValidCommand() {
        return ValidateTransferUseCase.ValidateTransferCommand.of(
                "123456", "789012", new BigDecimal("100.00"), "BRL", "test-key");
    }

    private void mockAccountExists(String accountNumber, String currency, BigDecimal balance) {
        var accountInfo = new AccountServicePort.AccountInfo(accountNumber, "Owner", balance, currency, true);
        when(accountServicePort.getAccount(accountNumber)).thenReturn(Optional.of(accountInfo));
    }
}
