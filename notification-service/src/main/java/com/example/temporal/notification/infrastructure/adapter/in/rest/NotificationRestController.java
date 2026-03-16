package com.example.temporal.notification.infrastructure.adapter.in.rest;

import com.example.temporal.notification.domain.model.NotificationDomain;
import com.example.temporal.notification.domain.port.in.SendNotificationUseCase;
import com.example.temporal.notification.domain.port.in.QueryNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller adapter for notification operations
 * Exposes domain use cases as HTTP endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final SendNotificationUseCase sendNotificationUseCase;
    private final QueryNotificationUseCase queryNotificationUseCase;

    /**
     * Sends a notification manually (for testing)
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestBody SendNotificationRequest request) {
        
        log.info("REST API: Sending notification for event: {}", request.eventType());

        var command = SendNotificationUseCase.SendNotificationCommand.of(
                request.eventType(),
                request.transferId(),
                request.accountNumber(),
                request.message(),
                request.recipient(),
                request.idempotencyKey()
        );

        var result = sendNotificationUseCase.sendNotification(command);

        var response = new NotificationResponse(
                result.notificationId(),
                result.success(),
                result.errorMessage()
        );

        return result.success() 
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Gets notification by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getNotificationById(
            @PathVariable Long id) {
        log.info("REST API: Getting notification by ID: {}", id);

        Optional<NotificationDomain> notification = queryNotificationUseCase.getNotificationById(id);

        return notification.map(n -> ResponseEntity.ok(toDetailResponse(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets notifications by transfer ID
     */
    @GetMapping("/transfer/{transferId}")
    public ResponseEntity<List<NotificationSummaryResponse>> getNotificationsByTransferId(
            @PathVariable String transferId) {
        log.info("REST API: Getting notifications by transfer ID: {}", transferId);

        List<NotificationDomain> notifications = 
                queryNotificationUseCase.getNotificationsByTransferId(transferId);

        return ResponseEntity.ok(notifications.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets notifications by account number
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<NotificationSummaryResponse>> getNotificationsByAccount(
            @PathVariable String accountNumber) {
        log.info("REST API: Getting notifications by account: {}", accountNumber);

        List<NotificationDomain> notifications = 
                queryNotificationUseCase.getNotificationsByAccount(accountNumber);

        return ResponseEntity.ok(notifications.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets notifications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationSummaryResponse>> getNotificationsByStatus(
            @PathVariable String status) {
        log.info("REST API: Getting notifications by status: {}", status);

        try {
            NotificationDomain.NotificationStatus notificationStatus = 
                    NotificationDomain.NotificationStatus.valueOf(status.toUpperCase());
            
            List<NotificationDomain> notifications = 
                    queryNotificationUseCase.getNotificationsByStatus(notificationStatus);

            return ResponseEntity.ok(notifications.stream()
                    .map(this::toSummaryResponse)
                    .collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private NotificationDetailResponse toDetailResponse(NotificationDomain n) {
        return new NotificationDetailResponse(
                n.getId(),
                n.getEventType(),
                n.getTransferId(),
                n.getAccountNumber(),
                n.getMessage(),
                n.getRecipient(),
                n.getStatus().name(),
                n.getSentAt(),
                n.getCreatedAt(),
                n.getIdempotencyKey()
        );
    }

    private NotificationSummaryResponse toSummaryResponse(NotificationDomain n) {
        return new NotificationSummaryResponse(
                n.getId(),
                n.getEventType(),
                n.getTransferId(),
                n.getMessage(),
                n.getStatus().name(),
                n.getSentAt()
        );
    }

    // Request/Response DTOs

    public record SendNotificationRequest(
            String eventType,
            String transferId,
            String accountNumber,
            String message,
            String recipient,
            String idempotencyKey
    ) {}

    public record NotificationResponse(
            Long notificationId,
            Boolean success,
            String errorMessage
    ) {}

    public record NotificationDetailResponse(
            Long id,
            String eventType,
            String transferId,
            String accountNumber,
            String message,
            String recipient,
            String status,
            java.time.LocalDateTime sentAt,
            java.time.LocalDateTime createdAt,
            String idempotencyKey
    ) {}

    public record NotificationSummaryResponse(
            Long id,
            String eventType,
            String transferId,
            String message,
            String status,
            java.time.LocalDateTime sentAt
    ) {}
}
