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
    public void handleTransferEvent(final String event) {

        log.info("Received transfer event: {}", event);
        String[] parts = validatingReceivedPayload(event);
        if (parts == null) return;

        try {

            String eventType = parts[0];
            Long transferId = Long.parseLong(parts[1]);

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

        } catch (Exception e) {
            log.error("Error while handling transfer event: {}", event, e);
        }

    }

    private static String[] validatingReceivedPayload(final String event) {

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
}