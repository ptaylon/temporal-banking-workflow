package com.example.temporal.audit.infrastructure.adapter.out.persistence;

import com.example.temporal.audit.domain.model.AuditEventDomain;
import com.example.temporal.audit.entity.AuditEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapper between domain model and JPA entity
 * Note: Uses manual mapping due to complex JSON serialization requirements
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventMapper {

    private final ObjectMapper objectMapper;

    /**
     * Converts domain model to JPA entity
     */
    public AuditEventEntity toEntity(AuditEventDomain domain) {
        if (domain == null) {
            return null;
        }

        AuditEventEntity entity = new AuditEventEntity();
        entity.setId(domain.getId());
        entity.setEventType(domain.getEventType());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setBeforeState(mapToJson(domain.getBeforeState()));
        entity.setAfterState(mapToJson(domain.getAfterState()));
        entity.setUserId(domain.getUserId());
        entity.setTimestamp(domain.getTimestamp());
        entity.setIdempotencyKey(domain.getIdempotencyKey());

        return entity;
    }

    /**
     * Converts JPA entity to domain model
     */
    public AuditEventDomain toDomain(AuditEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return AuditEventDomain.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .beforeState(mapFromJson(entity.getBeforeState()))
                .afterState(mapFromJson(entity.getAfterState()))
                .userId(entity.getUserId())
                .timestamp(entity.getTimestamp())
                .idempotencyKey(entity.getIdempotencyKey())
                .build();
    }

    /**
     * Converts Map to JSON string
     */
    protected String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Error converting map to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts JSON string to Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Error converting JSON to map: {}", e.getMessage());
            return null;
        }
    }
}
