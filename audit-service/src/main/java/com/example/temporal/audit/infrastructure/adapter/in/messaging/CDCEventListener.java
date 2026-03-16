package com.example.temporal.audit.infrastructure.adapter.in.messaging;

import com.example.temporal.audit.domain.port.in.ProcessCDCEventUseCase;
import com.example.temporal.audit.domain.port.out.CDCEventParserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener adapter for CDC events
 * Consumes Debezium CDC events and processes them for audit
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CDCEventListener {

    private final ProcessCDCEventUseCase processCDCEventUseCase;
    private final CDCEventParserPort cdcEventParserPort;

    /**
     * Handles CDC events from Kafka
     * Listens to Debezium topics for accounts and transfers tables
     */
    @KafkaListener(
            topics = {"banking.public.accounts", "banking.public.transfers"},
            groupId = "audit-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCdcEvent(
            @Payload String message,
            @Header("kafka_receivedTopic") String topic,
            Acknowledgment acknowledgment) {

        log.info("Received CDC event from topic: {}", topic);
        log.debug("Raw message: {}", message);

        try {
            // Parse the CDC event
            var parsedEvent = cdcEventParserPort.parse(message, topic);

            if (!parsedEvent.isValid()) {
                log.warn("Invalid CDC event from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            // Generate idempotency key from topic and entity ID
            String idempotencyKey = topic + "-" + parsedEvent.entityId() + "-" + System.currentTimeMillis();

            // Create command and process event
            var command = ProcessCDCEventUseCase.ProcessCDCEventCommand.of(
                    parsedEvent.topic(),
                    parsedEvent.operation(),
                    parsedEvent.entityType(),
                    parsedEvent.entityId(),
                    parsedEvent.beforeState(),
                    parsedEvent.afterState(),
                    idempotencyKey
            );

            var result = processCDCEventUseCase.processCDCEvent(command);

            if (result.success()) {
                log.info("Successfully processed CDC event: {} for {}/{}", 
                        parsedEvent.operation(), parsedEvent.entityType(), parsedEvent.entityId());
            } else {
                log.warn("Failed to process CDC event: {}. Error: {}", 
                        parsedEvent.entityId(), result.errorMessage());
            }

            // Acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing CDC event from topic {}: {}", topic, e.getMessage(), e);
            log.debug("Problematic message: {}", message);
            // Acknowledge even on error to avoid infinite loop
            acknowledgment.acknowledge();
        }
    }
}
