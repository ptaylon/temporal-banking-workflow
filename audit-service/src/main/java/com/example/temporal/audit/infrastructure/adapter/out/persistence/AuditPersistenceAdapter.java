package com.example.temporal.audit.infrastructure.adapter.out.persistence;

import com.example.temporal.audit.domain.model.AuditEventDomain;
import com.example.temporal.audit.domain.port.out.AuditPersistencePort;
import com.example.temporal.audit.entity.AuditEventEntity;
import com.example.temporal.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter for audit events
 * Implements the AuditPersistencePort using JPA
 */
@Component
@RequiredArgsConstructor
public class AuditPersistenceAdapter implements AuditPersistencePort {

    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;

    @Override
    public AuditEventDomain save(final AuditEventDomain domain) {
        final AuditEventEntity entity = mapper.toEntity(domain);
        final AuditEventEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AuditEventDomain> findById(final Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AuditEventDomain> findByIdempotencyKey(final String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<AuditEventDomain> findByEntityTypeAndEntityId(
            final String entityType,
            final String entityId) {
        return repository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEventDomain> findByEntityType(final String entityType) {
        return repository.findByEntityType(entityType)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEventDomain> findByEventType(final String eventType) {
        return repository.findByEventType(eventType)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEventDomain> findByUserId(final String userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEventDomain> findByEntityTypeAndEventTypesInRange(
            final String entityType,
            final List<String> eventTypes,
            final LocalDateTime start,
            final LocalDateTime end) {
        return repository.findByEntityTypeAndEventTypesInRange(entityType, eventTypes, start, end)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
