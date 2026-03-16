package com.example.temporal.audit.domain.port.out;

import com.example.temporal.audit.domain.model.AuditEventDomain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port for audit event persistence operations
 * Defines what the domain needs from the infrastructure
 */
public interface AuditPersistencePort {

    /**
     * Saves an audit event
     * @param event the audit event to save
     * @return the saved audit event
     */
    AuditEventDomain save(AuditEventDomain event);

    /**
     * Finds an audit event by ID
     * @param id the audit event ID
     * @return the audit event if found
     */
    Optional<AuditEventDomain> findById(Long id);

    /**
     * Finds an audit event by idempotency key
     * @param idempotencyKey the idempotency key (Kafka offset based)
     * @return the audit event if found
     */
    Optional<AuditEventDomain> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds audit events by entity type and ID
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit events
     */
    List<AuditEventDomain> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Finds audit events by entity type
     * @param entityType the entity type
     * @return list of audit events
     */
    List<AuditEventDomain> findByEntityType(String entityType);

    /**
     * Finds audit events by event type
     * @param eventType the event type
     * @return list of audit events
     */
    List<AuditEventDomain> findByEventType(String eventType);

    /**
     * Finds audit events by entity type and event types in date range
     * @param entityType the entity type
     * @param eventTypes the event types
     * @param start start date
     * @param end end date
     * @return list of audit events
     */
    List<AuditEventDomain> findByEntityTypeAndEventTypesInRange(
            String entityType,
            List<String> eventTypes,
            LocalDateTime start,
            LocalDateTime end);

    /**
     * Finds audit events by user ID
     * @param userId the user ID
     * @return list of audit events
     */
    List<AuditEventDomain> findByUserId(String userId);
}
