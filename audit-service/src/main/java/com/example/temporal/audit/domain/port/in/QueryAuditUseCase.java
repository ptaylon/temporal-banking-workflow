package com.example.temporal.audit.domain.port.in;

import com.example.temporal.audit.domain.model.AuditEventDomain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Use case for querying audit events
 * Defines what the system can do regarding audit queries
 */
public interface QueryAuditUseCase {

    /**
     * Gets an audit event by ID
     * @param auditEventId the audit event ID
     * @return the audit event if found
     */
    Optional<AuditEventDomain> getAuditEventById(Long auditEventId);

    /**
     * Gets audit events by entity type and ID
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit events
     */
    List<AuditEventDomain> getAuditEventsByEntity(String entityType, String entityId);

    /**
     * Gets audit events by entity type
     * @param entityType the entity type
     * @return list of audit events
     */
    List<AuditEventDomain> getAuditEventsByEntityType(String entityType);

    /**
     * Gets audit events by event type
     * @param eventType the event type
     * @return list of audit events
     */
    List<AuditEventDomain> getAuditEventsByEventType(String eventType);

    /**
     * Gets audit events by entity type and event types in date range
     * @param entityType the entity type
     * @param eventTypes the event types
     * @param start start date
     * @param end end date
     * @return list of audit events
     */
    List<AuditEventDomain> getAuditEventsByTypeInRange(
            String entityType,
            List<String> eventTypes,
            LocalDateTime start,
            LocalDateTime end);

    /**
     * Gets audit events by user ID
     * @param userId the user ID
     * @return list of audit events
     */
    List<AuditEventDomain> getAuditEventsByUser(String userId);
}
