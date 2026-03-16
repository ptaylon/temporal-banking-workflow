package com.example.temporal.audit.domain.port.in;

import com.example.temporal.audit.domain.model.AuditEventDomain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case for processing CDC events
 * Defines what the system can do regarding CDC event processing
 */
public interface ProcessCDCEventUseCase {

    /**
     * Processes a CDC event from Kafka
     * @param command the CDC event command
     * @return the result of the processing
     */
    ProcessCDCEventResult processCDCEvent(ProcessCDCEventCommand command);

    /**
     * Command object for processing CDC events
     */
    record ProcessCDCEventCommand(
            String topic,
            String operation,
            String entityType,
            String entityId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String idempotencyKey
    ) {
        public ProcessCDCEventCommand {
            if (topic == null || topic.trim().isEmpty()) {
                throw new IllegalArgumentException("Topic cannot be null or empty");
            }
            if (operation == null || operation.trim().isEmpty()) {
                throw new IllegalArgumentException("Operation cannot be null or empty");
            }
            if (entityType == null || entityType.trim().isEmpty()) {
                throw new IllegalArgumentException("Entity type cannot be null or empty");
            }
            if (entityId == null || entityId.trim().isEmpty()) {
                throw new IllegalArgumentException("Entity ID cannot be null or empty");
            }
        }

        public static ProcessCDCEventCommand of(
                String topic,
                String operation,
                String entityType,
                String entityId,
                Map<String, Object> beforeState,
                Map<String, Object> afterState,
                String idempotencyKey) {
            return new ProcessCDCEventCommand(
                    topic,
                    operation,
                    entityType,
                    entityId,
                    beforeState,
                    afterState,
                    idempotencyKey
            );
        }
    }

    /**
     * Result object for CDC event processing
     */
    record ProcessCDCEventResult(
            boolean success,
            String errorMessage,
            Long auditEventId
    ) {
        public static ProcessCDCEventResult success(Long auditEventId) {
            return new ProcessCDCEventResult(true, null, auditEventId);
        }

        public static ProcessCDCEventResult failure(String errorMessage) {
            return new ProcessCDCEventResult(false, errorMessage, null);
        }
    }
}
