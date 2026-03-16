package com.example.temporal.notification.domain.service;

import com.example.temporal.notification.domain.model.NotificationDomain;
import com.example.temporal.notification.domain.port.in.SendNotificationUseCase;
import com.example.temporal.notification.domain.port.out.EmailPort;
import com.example.temporal.notification.domain.port.out.NotificationPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 * Tests the domain service without Spring context or infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationPersistencePort notificationPersistencePort;

    @Mock
    private EmailPort emailPort;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationPersistencePort,
                emailPort
        );
    }

    @Test
    @DisplayName("Should send notification successfully with email")
    void shouldSendNotificationSuccessfullyWithEmail() {
        // Arrange
        var command = createValidCommandWithEmail();
        
        // Mock no existing notification (not a duplicate)
        when(notificationPersistencePort.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        
        // Mock email send success
        when(emailPort.sendEmail(any(), any(), any()))
                .thenReturn(true);
        
        // Mock persistence save
        var savedNotification = NotificationDomain.builder()
                .id(1L)
                .status(NotificationDomain.NotificationStatus.SENT)
                .build();
        when(notificationPersistencePort.save(any()))
                .thenReturn(savedNotification);

        // Act
        var result = notificationService.sendNotification(command);

        // Assert
        assertTrue(result.success(), "Notification should be sent successfully");
        assertNotNull(result.notificationId(), "Notification ID should not be null");
        
        verify(notificationPersistencePort, times(2)).save(any()); // Save pending, then save sent
        verify(emailPort).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should send notification without email (logging only)")
    void shouldSendNotificationWithoutEmail() {
        // Arrange
        var command = createValidCommandWithoutEmail();
        
        // Mock no existing notification
        when(notificationPersistencePort.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        
        // Mock persistence save
        var savedNotification = NotificationDomain.builder()
                .id(1L)
                .status(NotificationDomain.NotificationStatus.SENT)
                .build();
        when(notificationPersistencePort.save(any()))
                .thenReturn(savedNotification);

        // Act
        var result = notificationService.sendNotification(command);

        // Assert
        assertTrue(result.success(), "Notification should be sent successfully");
        
        // Email should NOT be called (no recipient)
        verify(emailPort, never()).sendEmail(any(), any(), any());
        verify(notificationPersistencePort, times(2)).save(any());
    }

    @Test
    @DisplayName("Should handle email send failure")
    void shouldHandleEmailSendFailure() {
        // Arrange
        var command = createValidCommandWithEmail();
        
        // Mock no existing notification
        when(notificationPersistencePort.findByIdempotencyKey(any()))
                .thenReturn(Optional.empty());
        
        // Mock email send failure
        when(emailPort.sendEmail(any(), any(), any()))
                .thenReturn(false);
        
        // Mock persistence save
        var failedNotification = NotificationDomain.builder()
                .id(1L)
                .status(NotificationDomain.NotificationStatus.FAILED)
                .build();
        when(notificationPersistencePort.save(any()))
                .thenReturn(failedNotification);

        // Act
        var result = notificationService.sendNotification(command);

        // Assert
        assertFalse(result.success(), "Notification send should fail");
        assertNotNull(result.errorMessage(), "Error message should not be null");
        
        verify(notificationPersistencePort, times(2)).save(any()); // Save pending, then save failed
        verify(emailPort).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should return existing notification for duplicate request (idempotency)")
    void shouldReturnExistingNotificationForDuplicateRequest() {
        // Arrange
        var command = createValidCommandWithEmail();
        
        // Mock existing notification
        var existingNotification = NotificationDomain.builder()
                .id(99L)
                .status(NotificationDomain.NotificationStatus.SENT)
                .build();
        
        when(notificationPersistencePort.findByIdempotencyKey("test-key"))
                .thenReturn(Optional.of(existingNotification));

        // Act
        var result = notificationService.sendNotification(command);

        // Assert
        assertTrue(result.success(), "Should return existing notification");
        assertEquals(99L, result.notificationId(), "Should return existing notification ID");
        
        // Verify save was NOT called (idempotent - returned existing)
        verify(notificationPersistencePort, never()).save(any());
        verify(emailPort, never()).sendEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Should get notification by ID")
    void shouldGetNotificationById() {
        // Arrange
        var notification = NotificationDomain.builder()
                .id(1L)
                .eventType("TRANSFER_COMPLETED")
                .build();
        
        when(notificationPersistencePort.findById(1L))
                .thenReturn(Optional.of(notification));

        // Act
        var result = notificationService.getNotificationById(1L);

        // Assert
        assertTrue(result.isPresent(), "Notification should be found");
        assertEquals(1L, result.get().getId(), "ID should match");
    }

    @Test
    @DisplayName("Should return empty when notification not found")
    void shouldReturnEmptyWhenNotificationNotFound() {
        // Arrange
        when(notificationPersistencePort.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        var result = notificationService.getNotificationById(999L);

        // Assert
        assertFalse(result.isPresent(), "Notification should not be found");
    }

    @Test
    @DisplayName("Should get notifications by transfer ID")
    void shouldGetNotificationsByTransferId() {
        // Arrange
        var notifications = java.util.List.of(
                NotificationDomain.builder().id(1L).transferId("transfer-1").build(),
                NotificationDomain.builder().id(2L).transferId("transfer-1").build()
        );
        
        when(notificationPersistencePort.findByTransferId("transfer-1"))
                .thenReturn(notifications);

        // Act
        var result = notificationService.getNotificationsByTransferId("transfer-1");

        // Assert
        assertEquals(2, result.size(), "Should return 2 notifications");
    }

    @Test
    @DisplayName("Should get notifications by account number")
    void shouldGetNotificationsByAccountNumber() {
        // Arrange
        var notifications = java.util.List.of(
                NotificationDomain.builder().id(1L).accountNumber("123456").build()
        );
        
        when(notificationPersistencePort.findByAccountNumber("123456"))
                .thenReturn(notifications);

        // Act
        var result = notificationService.getNotificationsByAccount("123456");

        // Assert
        assertEquals(1, result.size(), "Should return 1 notification");
    }

    @Test
    @DisplayName("Should get notifications by event type")
    void shouldGetNotificationsByEventType() {
        // Arrange
        var notifications = java.util.List.of(
                NotificationDomain.builder().id(1L).eventType("TRANSFER_COMPLETED").build()
        );
        
        when(notificationPersistencePort.findByEventType("TRANSFER_COMPLETED"))
                .thenReturn(notifications);

        // Act
        var result = notificationService.getNotificationsByEventType("TRANSFER_COMPLETED");

        // Assert
        assertEquals(1, result.size(), "Should return 1 notification");
    }

    @Test
    @DisplayName("Should get notifications by status")
    void shouldGetNotificationsByStatus() {
        // Arrange
        var notifications = java.util.List.of(
                NotificationDomain.builder()
                        .id(1L)
                        .status(NotificationDomain.NotificationStatus.SENT)
                        .build()
        );
        
        when(notificationPersistencePort.findByStatus(NotificationDomain.NotificationStatus.SENT))
                .thenReturn(notifications);

        // Act
        var result = notificationService.getNotificationsByStatus(
                NotificationDomain.NotificationStatus.SENT
        );

        // Assert
        assertEquals(1, result.size(), "Should return 1 notification");
    }

    @Test
    @DisplayName("Should throw exception for invalid command")
    void shouldThrowExceptionForInvalidCommand() {
        // Act & Assert - command validation throws when creating command with empty event type
        var exception = assertThrows(IllegalArgumentException.class,
                () -> SendNotificationUseCase.SendNotificationCommand.of(
                        "", // Empty event type - will fail validation
                        "transfer-1",
                        "123456",
                        "Test message",
                        "test@example.com",
                        "test-key"
                ));
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    // Helper methods

    private SendNotificationUseCase.SendNotificationCommand createValidCommandWithEmail() {
        return SendNotificationUseCase.SendNotificationCommand.of(
                "TRANSFER_COMPLETED",
                "transfer-1",
                "123456",
                "Your transfer has been completed successfully",
                "user@example.com",
                "test-key"
        );
    }

    private SendNotificationUseCase.SendNotificationCommand createValidCommandWithoutEmail() {
        return SendNotificationUseCase.SendNotificationCommand.of(
                "TRANSFER_COMPLETED",
                "transfer-1",
                "123456",
                "Your transfer has been completed successfully",
                null, // No recipient
                "test-key"
        );
    }
}
