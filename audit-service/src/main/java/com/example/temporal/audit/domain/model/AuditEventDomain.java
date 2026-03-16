package com.example.temporal.audit.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Pure domain model for Audit Event - framework independent
 * Represents the core business concept of an audit event
 */
@Value
@Builder
@With
public class AuditEventDomain {
    Long id;
    String eventType;
    String entityType;
    String entityId;
    Map<String, Object> beforeState;
    Map<String, Object> afterState;
    String userId;
    LocalDateTime timestamp;
    String idempotencyKey;

    /**
     * Business validation: ensures audit event data is valid
     */
    public void validate() {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type cannot be null or empty");
        }
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity ID cannot be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    /**
     * Business rule: is this a create event?
     */
    public boolean isCreateEvent() {
        return eventType.endsWith("_CREATED");
    }

    /**
     * Business rule: is this an update event?
     */
    public boolean isUpdateEvent() {
        return eventType.endsWith("_UPDATED");
    }

    /**
     * Business rule: is this a delete event?
     */
    public boolean isDeleteEvent() {
        return eventType.endsWith("_DELETED");
    }

    /**
     * Business rule: has the entity changed?
     */
    public boolean hasChanges() {
        if (beforeState == null && afterState == null) {
            return false;
        }
        if (beforeState == null || afterState == null) {
            return true;
        }
        return !beforeState.equals(afterState);
    }

    /**
     * Creates a new audit event
     */
    public static AuditEventDomain create(
            String eventType,
            String entityType,
            String entityId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String userId,
            String idempotencyKey) {

        AuditEventDomain event = AuditEventDomain.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .beforeState(beforeState)
                .afterState(afterState)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();

        event.validate();
        return event;
    }

    /**
     * Gets the changed fields between before and after state
     */
    public java.util.Set<String> getChangedFields() {
        if (beforeState == null || afterState == null) {
            return java.util.Collections.emptySet();
        }

        java.util.Set<String> changedFields = new java.util.HashSet<>();
        
        // Check for modified or added fields
        for (String key : afterState.keySet()) {
            Object beforeValue = beforeState.get(key);
            Object afterValue = afterState.get(key);
            if (!java.util.Objects.equals(beforeValue, afterValue)) {
                changedFields.add(key);
            }
        }

        // Check for removed fields
        for (String key : beforeState.keySet()) {
            if (!afterState.containsKey(key)) {
                changedFields.add(key);
            }
        }

        return changedFields;
    }
}
