package com.example.temporal.audit.domain.port.out;

import java.util.Map;

/**
 * Port for CDC event parsing operations
 * Defines what the domain needs for parsing Debezium CDC events
 */
public interface CDCEventParserPort {

    /**
     * Parses a CDC event message from Kafka
     * @param message the raw Kafka message
     * @param topic the Kafka topic
     * @return the parsed CDC event
     */
    ParsedCDCEvent parse(String message, String topic);

    /**
     * Parsed CDC event DTO
     */
    record ParsedCDCEvent(
            String topic,
            String operation,
            String entityType,
            String entityId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            boolean isValid
    ) {
        public static ParsedCDCEvent invalid(String reason) {
            return new ParsedCDCEvent(null, null, null, null, null, null, false);
        }

        public static ParsedCDCEvent valid(
                String topic,
                String operation,
                String entityType,
                String entityId,
                Map<String, Object> beforeState,
                Map<String, Object> afterState) {
            return new ParsedCDCEvent(topic, operation, entityType, entityId, beforeState, afterState, true);
        }
    }
}
