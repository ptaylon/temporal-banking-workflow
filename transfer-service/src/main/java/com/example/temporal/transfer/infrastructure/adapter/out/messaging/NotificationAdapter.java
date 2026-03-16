package com.example.temporal.transfer.infrastructure.adapter.out.messaging;

import com.example.temporal.transfer.domain.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter for notification operations
 * Implements domain port using Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationPort {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    @Override
    public void notifyTransferInitiated(Long transferId) {
        log.debug("Notifying transfer initiated: {}", transferId);

        try {
            String event = String.format("TRANSFER_INITIATED:%d", transferId);
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, event);

            log.debug("Notification sent for initiated transfer: {}", transferId);

        } catch (Exception e) {
            log.error("Failed to send notification for transfer {}: {}", transferId, e.getMessage());
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void notifyTransferCompleted(Long transferId) {
        log.debug("Notifying transfer completed: {}", transferId);

        try {
            String event = String.format("TRANSFER_COMPLETED:%d", transferId);
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, event);

            log.debug("Notification sent for completed transfer: {}", transferId);

        } catch (Exception e) {
            log.error("Failed to send notification for transfer {}: {}", transferId, e.getMessage());
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void notifyTransferFailed(Long transferId, String reason) {
        log.debug("Notifying transfer failed: {} reason: {}", transferId, reason);

        try {
            String event = String.format("TRANSFER_FAILED:%d:%s", transferId, reason);
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, event);

            log.debug("Notification sent for failed transfer: {}", transferId);

        } catch (Exception e) {
            log.error("Failed to send notification for transfer {}: {}", transferId, e.getMessage());
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }
}
