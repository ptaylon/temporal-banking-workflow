package com.example.temporal.audit.infrastructure.adapter.in.rest;

import com.example.temporal.audit.domain.model.AuditEventDomain;
import com.example.temporal.audit.domain.port.in.QueryAuditUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller adapter for audit operations
 * Exposes domain use cases as HTTP endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditRestController {

    private final QueryAuditUseCase queryAuditUseCase;

    /**
     * Gets audit history for an entity
     */
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditEventResponse>> getEntityAuditHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        log.info("REST API: Getting audit history for {}/{}", entityType, entityId);

        List<AuditEventDomain> events = queryAuditUseCase.getAuditEventsByEntity(entityType, entityId);

        return ResponseEntity.ok(events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Searches audit events by type and date range
     */
    @GetMapping("/search")
    public ResponseEntity<List<AuditEventResponse>> searchAuditEvents(
            @RequestParam String entityType,
            @RequestParam List<String> eventTypes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("REST API: Searching audit events for {} in range {} to {}", eventTypes, start, end);

        List<AuditEventDomain> events = queryAuditUseCase.getAuditEventsByTypeInRange(
                entityType, eventTypes, start, end);

        return ResponseEntity.ok(events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets audit events by entity type
     */
    @GetMapping("/entity-type/{entityType}")
    public ResponseEntity<List<AuditEventResponse>> getAuditEventsByEntityType(
            @PathVariable String entityType) {
        log.info("REST API: Getting audit events by entity type: {}", entityType);

        List<AuditEventDomain> events = queryAuditUseCase.getAuditEventsByEntityType(entityType);

        return ResponseEntity.ok(events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets audit events by event type
     */
    @GetMapping("/event-type/{eventType}")
    public ResponseEntity<List<AuditEventResponse>> getAuditEventsByEventType(
            @PathVariable String eventType) {
        log.info("REST API: Getting audit events by event type: {}", eventType);

        List<AuditEventDomain> events = queryAuditUseCase.getAuditEventsByEventType(eventType);

        return ResponseEntity.ok(events.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets audit event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditEventDetailResponse> getAuditEventById(
            @PathVariable Long id) {
        log.info("REST API: Getting audit event by ID: {}", id);

        return queryAuditUseCase.getAuditEventById(id)
                .map(event -> ResponseEntity.ok(toDetailResponse(event)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AuditEventResponse toResponse(AuditEventDomain event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getTimestamp(),
                event.getChangedFields()
        );
    }

    private AuditEventDetailResponse toDetailResponse(AuditEventDomain event) {
        return new AuditEventDetailResponse(
                event.getId(),
                event.getEventType(),
                event.getEntityType(),
                event.getEntityId(),
                event.getBeforeState(),
                event.getAfterState(),
                event.getUserId(),
                event.getTimestamp(),
                event.getIdempotencyKey(),
                event.getChangedFields()
        );
    }

    // Response DTOs

    public record AuditEventResponse(
            Long id,
            String eventType,
            String entityType,
            String entityId,
            LocalDateTime timestamp,
            java.util.Set<String> changedFields
    ) {}

    public record AuditEventDetailResponse(
            Long id,
            String eventType,
            String entityType,
            String entityId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState,
            String userId,
            LocalDateTime timestamp,
            String idempotencyKey,
            java.util.Set<String> changedFields
    ) {}
}
