package com.example.temporal.notification.kafka;

import com.example.temporal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transfer-events", groupId = "notification-service")
    public void handleTransferEvent(String event) {
        log.info("Received transfer event: {}", event);
        if (event == null || event.isBlank()) {
            log.warn("Received empty or null event. Skipping.");
            return;
        }

        String[] parts = event.split(":", 3); // limit to avoid splitting reason containing colons
        if (parts.length < 2) {
            log.warn("Malformed event payload (expected at least 2 parts): {}", event);
            return;
        }

        String eventType = parts[0];
        Long transferId;
        try {
            transferId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("Invalid transferId in event: {}", event, e);
            return;
        }

        try {
            switch (eventType) {
                case "TRANSFER_INITIATED":
                    notificationService.sendTransferInitiatedNotification(transferId);
                    break;
                case "TRANSFER_COMPLETED":
                    notificationService.sendTransferCompletedNotification(transferId);
                    break;
                case "TRANSFER_FAILED":
                    String reason = parts.length > 2 ? parts[2] : "Unknown reason";
                    notificationService.sendTransferFailedNotification(transferId, reason);
                    break;
                default:
                    log.warn("Unknown transfer event type: {}", eventType);
            }
        } catch (Exception ex) {
            // Protect the Kafka consumer from crashing due to downstream failures
            log.error("Error while handling transfer event: {}", event, ex);
        }
    }
}