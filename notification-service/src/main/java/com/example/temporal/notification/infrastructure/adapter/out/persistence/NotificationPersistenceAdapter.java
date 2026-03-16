package com.example.temporal.notification.infrastructure.adapter.out.persistence;

import com.example.temporal.notification.domain.model.NotificationDomain;
import com.example.temporal.notification.domain.port.out.NotificationPersistencePort;
import com.example.temporal.notification.entity.NotificationEntity;
import com.example.temporal.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter for notification
 * Implements the NotificationPersistencePort using JPA
 */
@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationPersistencePort {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Override
    public NotificationDomain save(final NotificationDomain domain) {
        final NotificationEntity entity = mapper.toEntity(domain);
        final NotificationEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<NotificationDomain> findById(final Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<NotificationDomain> findByIdempotencyKey(final String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<NotificationDomain> findByTransferId(final String transferId) {
        return repository.findByTransferId(transferId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDomain> findByAccountNumber(final String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDomain> findByEventType(final String eventType) {
        return repository.findByEventType(eventType)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDomain> findByStatus(
            final NotificationDomain.NotificationStatus status) {
        return repository.findByNotificationStatus(toEntityStatus(status))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Converts domain status to entity status
     */
    private NotificationEntity.NotificationStatus toEntityStatus(
            NotificationDomain.NotificationStatus domainStatus) {
        if (domainStatus == null) {
            return null;
        }
        return NotificationEntity.NotificationStatus.valueOf(domainStatus.name());
    }
}
