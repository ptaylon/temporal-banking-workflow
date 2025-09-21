package com.example.temporal.audit.repository;

import com.example.temporal.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<AuditEvent> findByEntityTypeAndTimestampBetween(
            String entityType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.entityType = :entityType " +
           "AND ae.timestamp >= :start AND ae.timestamp <= :end " +
           "AND ae.eventType IN :eventTypes")
    List<AuditEvent> findByEntityTypeAndEventTypesInRange(
            @Param("entityType") String entityType,
            @Param("eventTypes") List<String> eventTypes,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}