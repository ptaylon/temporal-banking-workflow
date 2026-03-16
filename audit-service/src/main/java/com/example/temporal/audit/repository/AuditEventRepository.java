package com.example.temporal.audit.repository;

import com.example.temporal.audit.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for audit event entities
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    /**
     * Finds an audit event by idempotency key
     */
    Optional<AuditEventEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds audit events by entity type and ID
     */
    List<AuditEventEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    /**
     * Finds audit events by entity type
     */
    List<AuditEventEntity> findByEntityType(String entityType);

    /**
     * Finds audit events by event type
     */
    List<AuditEventEntity> findByEventType(String eventType);

    /**
     * Finds audit events by user ID
     */
    List<AuditEventEntity> findByUserId(String userId);

    /**
     * Finds audit events by entity type and event types in date range
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.entityType = :entityType " +
           "AND a.eventType IN :eventTypes " +
           "AND a.timestamp BETWEEN :start AND :end " +
           "ORDER BY a.timestamp DESC")
    List<AuditEventEntity> findByEntityTypeAndEventTypesInRange(
            @Param("entityType") String entityType,
            @Param("eventTypes") List<String> eventTypes,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
