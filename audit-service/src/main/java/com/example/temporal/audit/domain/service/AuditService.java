package com.example.temporal.audit.domain.service;

import com.example.temporal.audit.domain.model.AuditEventDomain;
import com.example.temporal.audit.domain.port.in.ProcessCDCEventUseCase;
import com.example.temporal.audit.domain.port.in.QueryAuditUseCase;
import com.example.temporal.audit.domain.port.out.AuditPersistencePort;
import com.example.temporal.audit.domain.port.out.CDCEventParserPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Domain service implementing audit use cases
 * Contains pure business logic without framework dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService implements ProcessCDCEventUseCase, QueryAuditUseCase {

    private final AuditPersistencePort auditPersistencePort;
    private final CDCEventParserPort cdcEventParserPort;

    @Override
    @Transactional
    public ProcessCDCEventResult processCDCEvent(ProcessCDCEventCommand command) {
        log.info("Processing CDC event from topic: {}, operation: {}, entity: {}/{}", 
                command.topic(), command.operation(), command.entityType(), command.entityId());

        try {
            // Check for duplicate event (idempotency based on Kafka offset)
            String idempotencyKey = command.idempotencyKey() != null
                    ? command.idempotencyKey()
                    : command.topic() + "-" + command.entityId() + "-" + System.currentTimeMillis();

            var existingEvent = auditPersistencePort.findByIdempotencyKey(idempotencyKey);
            if (existingEvent.isPresent()) {
                log.info("Audit event already exists for idempotency key: {}", idempotencyKey);
                return ProcessCDCEventResult.success(existingEvent.get().getId());
            }

            // Determine event type based on operation
            String eventType = determineEventType(command.operation(), command.entityType());

            // Create audit event domain object
            AuditEventDomain auditEvent = AuditEventDomain.create(
                    eventType,
                    command.entityType(),
                    command.entityId(),
                    command.beforeState(),
                    command.afterState(),
                    "SYSTEM", // In a real system, this would come from security context
                    idempotencyKey
            );

            // Validate that there are actual changes (for update events)
            if (auditEvent.isUpdateEvent() && !auditEvent.hasChanges()) {
                log.debug("Skipping audit for update with no changes: {}/{}", 
                        command.entityType(), command.entityId());
                return ProcessCDCEventResult.success(null);
            }

            // Save audit event
            AuditEventDomain savedEvent = auditPersistencePort.save(auditEvent);

            log.info("Audit event recorded: {} for {}/{}", 
                    eventType, command.entityType(), command.entityId());

            return ProcessCDCEventResult.success(savedEvent.getId());

        } catch (IllegalArgumentException e) {
            log.error("Validation error processing CDC event: {}", e.getMessage());
            return ProcessCDCEventResult.failure("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing CDC event: {}", e.getMessage(), e);
            return ProcessCDCEventResult.failure("Failed to process CDC event: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Optional<AuditEventDomain> getAuditEventById(final Long auditEventId) {
        log.debug("Getting audit event by ID: {}", auditEventId);
        return auditPersistencePort.findById(auditEventId);
    }

    @Override
    @Transactional
    public List<AuditEventDomain> getAuditEventsByEntity(
            final String entityType,
            final String entityId) {
        log.debug("Getting audit events by entity: {}/{}", entityType, entityId);
        return auditPersistencePort.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    @Transactional
    public List<AuditEventDomain> getAuditEventsByEntityType(final String entityType) {
        log.debug("Getting audit events by entity type: {}", entityType);
        return auditPersistencePort.findByEntityType(entityType);
    }

    @Override
    @Transactional
    public List<AuditEventDomain> getAuditEventsByEventType(final String eventType) {
        log.debug("Getting audit events by event type: {}", eventType);
        return auditPersistencePort.findByEventType(eventType);
    }

    @Override
    @Transactional
    public List<AuditEventDomain> getAuditEventsByTypeInRange(
            final String entityType,
            final List<String> eventTypes,
            final LocalDateTime start,
            final LocalDateTime end) {
        log.debug("Getting audit events by type in range: {} from {} to {}",
                eventTypes, start, end);
        return auditPersistencePort.findByEntityTypeAndEventTypesInRange(
                entityType, eventTypes, start, end);
    }

    @Override
    @Transactional
    public List<AuditEventDomain> getAuditEventsByUser(final String userId) {
        log.debug("Getting audit events by user: {}", userId);
        return auditPersistencePort.findByUserId(userId);
    }

    /**
     * Determines event type based on CDC operation
     */
    private String determineEventType(String operation, String entityType) {
        String upperEntityType = entityType.toUpperCase();
        
        return switch (operation) {
            case "c" -> upperEntityType + "_CREATED";
            case "u" -> upperEntityType + "_UPDATED";
            case "d" -> upperEntityType + "_DELETED";
            case "r" -> upperEntityType + "_READ"; // snapshot
            default -> upperEntityType + "_" + operation.toUpperCase();
        };
    }
}
