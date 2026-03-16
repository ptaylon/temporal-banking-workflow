package com.example.temporal.audit.infrastructure.adapter.out.parser;

import com.example.temporal.audit.domain.port.out.CDCEventParserPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for parsing CDC events from Debezium
 * Implements the CDCEventParserPort using Jackson ObjectMapper
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CDCEventParserAdapter implements CDCEventParserPort {

    private final ObjectMapper objectMapper;

    @Override
    public ParsedCDCEvent parse(String message, String topic) {
        try {
            // Validate message
            if (message == null || message.trim().isEmpty()) {
                log.warn("Received empty message from topic: {}", topic);
                return ParsedCDCEvent.invalid("Empty message");
            }

            JsonNode eventNode = objectMapper.readTree(message);
            log.debug("Parsing CDC event from topic: {}", topic);

            // Try different Debezium message formats
            JsonNode payload = null;
            String operation = null;
            JsonNode after = null;
            JsonNode before = null;

            // Standard Debezium Connect format with payload
            if (eventNode.has("payload")) {
                payload = eventNode.get("payload");
                operation = payload.has("op") ? payload.get("op").asText() : null;
                after = payload.get("after");
                before = payload.get("before");
            }
            // Alternative format - direct message with op
            else if (eventNode.has("op")) {
                operation = eventNode.get("op").asText();
                after = eventNode.get("after");
                before = eventNode.get("before");
            }
            // Unwrapped format - direct table data (no op, assume CREATE)
            else if (eventNode.has("id") || eventNode.size() > 0) {
                operation = "c"; // Assume CREATE by default
                after = eventNode;
                before = null;
                log.debug("Processing unwrapped message as CREATE operation");
            }
            // Unknown format
            else {
                log.warn("Unknown message format from topic: {}. Keys: {}", 
                        topic, eventNode.fieldNames());
                return ParsedCDCEvent.invalid("Unknown message format");
            }

            if (operation == null) {
                log.warn("No operation found in CDC event from topic: {}", topic);
                return ParsedCDCEvent.invalid("No operation found");
            }

            // Extract table name from topic
            String entityType = extractTableName(topic);

            // Convert before/after states to Maps
            Map<String, Object> beforeState = convertJsonNodeToMap(before);
            Map<String, Object> afterState = convertJsonNodeToMap(after);

            // Extract entity ID
            String entityId = extractEntityId(afterState, beforeState);

            log.debug("Parsed CDC event: operation={}, entityType={}, entityId={}", 
                    operation, entityType, entityId);

            return ParsedCDCEvent.valid(
                    topic,
                    operation,
                    entityType,
                    entityId,
                    beforeState,
                    afterState
            );

        } catch (Exception e) {
            log.error("Error parsing CDC event from topic {}: {}", topic, e.getMessage());
            return ParsedCDCEvent.invalid("Parse error: " + e.getMessage());
        }
    }

    /**
     * Extracts table name from Kafka topic
     * Topic format: banking.public.table_name
     */
    private String extractTableName(String topic) {
        String[] parts = topic.split("\\.");
        return parts.length >= 3 ? parts[2] : "unknown";
    }

    /**
     * Extracts entity ID from before/after states
     */
    private String extractEntityId(Map<String, Object> afterState, Map<String, Object> beforeState) {
        if (afterState != null && afterState.containsKey("id")) {
            return afterState.get("id").toString();
        } else if (beforeState != null && beforeState.containsKey("id")) {
            return beforeState.get("id").toString();
        }
        return "unknown";
    }

    /**
     * Converts JsonNode to Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Error converting JsonNode to Map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}
