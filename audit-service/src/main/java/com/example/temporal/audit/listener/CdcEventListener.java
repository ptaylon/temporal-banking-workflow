package com.example.temporal.audit.listener;

import com.example.temporal.audit.service.AuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdcEventListener {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"banking.public.accounts", "banking.public.transfers"})
    public void handleCdcEvent(
            @Payload String message, 
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received CDC event from topic: {}", topic);
            log.debug("Raw message: {}", message);

            // Verificar se a mensagem não está vazia
            if (message == null || message.trim().isEmpty()) {
                log.warn("Received empty message from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            JsonNode eventNode = objectMapper.readTree(message);
            
            // Log da estrutura da mensagem para debug
            log.debug("Message structure - keys: {}", eventNode.fieldNames());
            log.debug("Full message structure: {}", eventNode.toPrettyString());
            
            // Tentar diferentes estruturas de mensagem Debezium
            JsonNode payload = null;
            String operation = null;
            JsonNode after = null;
            JsonNode before = null;
            
            // Formato padrão Debezium Connect
            if (eventNode.has("payload")) {
                payload = eventNode.get("payload");
                operation = payload.has("op") ? payload.get("op").asText() : null;
                after = payload.get("after");
                before = payload.get("before");
            }
            // Formato alternativo - mensagem direta (com op)
            else if (eventNode.has("op")) {
                operation = eventNode.get("op").asText();
                after = eventNode.get("after");
                before = eventNode.get("before");
            }
            // Formato unwrapped - dados diretos da tabela (sem op)
            else if (eventNode.has("id") || eventNode.size() > 0) {
                // Assumir que é uma mensagem unwrapped (dados diretos da tabela)
                operation = "c"; // Assumir create por padrão
                after = eventNode;
                before = null;
                log.debug("Processing unwrapped message as CREATE operation");
            }
            // Formato desconhecido
            else {
                log.warn("Unknown message format from topic: {}. Keys: {}", topic, eventNode.fieldNames());
                log.debug("Full message: {}", message);
                acknowledgment.acknowledge();
                return;
            }

            if (operation == null) {
                log.warn("No operation found in CDC event from topic: {}", topic);
                acknowledgment.acknowledge();
                return;
            }

            String table = extractTableName(topic);
            log.debug("Processing operation: {} for table: {}", operation, table);
            
            Map<String, Object> afterState = after != null && !after.isNull() ? 
                convertJsonNodeToMap(after) : null;
            Map<String, Object> beforeState = before != null && !before.isNull() ? 
                convertJsonNodeToMap(before) : null;

            String entityId = extractEntityId(afterState, beforeState);
            String eventType = determineEventType(operation, table);
            
            // Log dos estados para debug
            log.debug("Before state: {}", beforeState);
            log.debug("After state: {}", afterState);
            
            auditService.recordEvent(
                eventType,
                table,
                entityId,
                beforeState,
                afterState,
                "SYSTEM" // Em um sistema real, isso viria do contexto de segurança
            );

            log.info("Successfully processed CDC event: {} for {}/{}", eventType, table, entityId);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing CDC event from topic {}: {}", topic, e.getMessage(), e);
            log.debug("Problematic message: {}", message);
            // Acknowledge mesmo com erro para evitar loop infinito
            acknowledgment.acknowledge();
        }
    }

    private String extractTableName(String topic) {
        // Topic format: banking.public.table_name
        String[] parts = topic.split("\\.");
        return parts.length >= 3 ? parts[2] : "unknown";
    }

    private String extractEntityId(Map<String, Object> afterState, Map<String, Object> beforeState) {
        if (afterState != null && afterState.containsKey("id")) {
            return afterState.get("id").toString();
        } else if (beforeState != null && beforeState.containsKey("id")) {
            return beforeState.get("id").toString();
        }
        return "unknown";
    }

    private String determineEventType(String operation, String table) {
        switch (operation) {
            case "c":
                return table.toUpperCase() + "_CREATED";
            case "u":
                return table.toUpperCase() + "_UPDATED";
            case "d":
                return table.toUpperCase() + "_DELETED";
            case "r":
                return table.toUpperCase() + "_READ"; // snapshot
            default:
                return "UNKNOWN_OPERATION";
        }
    }

    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        try {
            return objectMapper.convertValue(node, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Error converting JsonNode to Map: {}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }
}