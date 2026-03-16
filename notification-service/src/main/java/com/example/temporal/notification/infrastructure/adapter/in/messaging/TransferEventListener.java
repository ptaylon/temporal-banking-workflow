package com.example.temporal.notification.infrastructure.adapter.in.messaging;

import com.example.temporal.notification.domain.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener adapter for transfer events
 * Consumes transfer events and sends notifications
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventListener {

    private final SendNotificationUseCase sendNotificationUseCase;

    /**
     * Handles transfer events from Kafka
     * Expected format: "EVENT_TYPE:TRANSFER_ID:REASON" (reason optional)
     */
    @KafkaListener(topics = "transfer-events", groupId = "notification-service")
    public void handleTransferEvent(String event) {
        log.info("Received transfer event: {}", event);

        String[] parts = validateAndParseEvent(event);
        if (parts == null) {
            return;
        }

        try {
            String eventType = parts[0];
            String transferId = parts[1];
            String reason = parts.length > 2 ? parts[2] : null;

            // Build notification command
            var command = SendNotificationUseCase.SendNotificationCommand.of(
                    eventType,
                    transferId,
                    null, // accountNumber - would need to lookup from transfer
                    buildMessage(eventType, transferId, reason),
                    null, // recipient - would need to lookup from account
                    "kafka-" + eventType + "-" + transferId // idempotency key
            );

            // Send notification
            var result = sendNotificationUseCase.sendNotification(command);

            if (result.success()) {
                log.info("Notification sent successfully for event: {}, transfer: {}", 
                        eventType, transferId);
            } else {
                log.warn("Failed to send notification for event: {}, transfer: {}. Error: {}", 
                        eventType, transferId, result.errorMessage());
            }

        } catch (Exception e) {
            log.error("Error while handling transfer event: {}", event, e);
        }
    }

    /**
     * Validates and parses the event payload
     */
    private String[] validateAndParseEvent(String event) {
        if (event == null || event.isBlank()) {
            log.warn("Received empty or null event. Skipping.");
            return null;
        }

        String[] parts = event.split(":", 3); // limit to avoid splitting reason containing colons
        if (parts.length < 2) {
            log.warn("Malformed event payload (expected at least 2 parts): {}", event);
            return null;
        }

        return parts;
    }

    /**
     * Builds notification message based on event type
     */
    private String buildMessage(String eventType, String transferId, String reason) {
        return switch (eventType) {
            case "TRANSFER_INITIATED" -> 
                "Your transfer with ID " + transferId + " has been initiated.";
            case "TRANSFER_COMPLETED" -> 
                "Your transfer with ID " + transferId + " has been completed successfully.";
            case "TRANSFER_FAILED" -> 
                "Your transfer with ID " + transferId + " has failed. Reason: " + 
                (reason != null ? reason : "Unknown reason");
            case "TRANSFER_PAUSED" -> 
                "Your transfer with ID " + transferId + " has been paused.";
            case "TRANSFER_CANCELLED" -> 
                "Your transfer with ID " + transferId + " has been cancelled.";
            default -> 
                "Transfer event: " + eventType + " for transfer ID " + transferId;
        };
    }
}
