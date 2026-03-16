package com.example.temporal.transfer.infrastructure.adapter.out.persistence;

import com.example.temporal.common.model.Transfer;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.out.TransferPersistencePort;
import com.example.temporal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter for transfer persistence
 * Implements domain port using Spring Data JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferPersistenceAdapter implements TransferPersistencePort {

    private final TransferRepository repository;
    private final TransferMapper mapper;

    @Override
    public TransferDomain save(final TransferDomain domain) {
        log.debug("Saving transfer: {}", domain);

        final Transfer entity = mapper.toEntity(domain);
        final Transfer saved = repository.save(entity);

        log.debug("Transfer saved with ID: {}", saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public TransferDomain update(final TransferDomain domain) {
        log.debug("Updating transfer: {}", domain.getId());

        final Transfer entity = repository.findById(domain.getId())
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + domain.getId()));

        mapper.updateEntity(entity, domain);
        final Transfer updated = repository.save(entity);

        log.debug("Transfer updated: {}", updated.getId());
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<TransferDomain> findById(final Long id) {
        log.debug("Finding transfer by ID: {}", id);
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<TransferDomain> findByIdempotencyKey(final String idempotencyKey) {
        log.debug("Finding transfer by idempotency key: {}", idempotencyKey);
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public List<TransferDomain> findByAccountNumber(final String accountNumber) {
        log.debug("Finding transfers by account number: {}", accountNumber);
        return repository.findByAccountNumber(accountNumber).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferDomain> findByStatus(final TransferStatus status) {
        log.debug("Finding transfers by status: {}", status);
        return repository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIdempotencyKey(final String idempotencyKey) {
        log.debug("Checking if transfer exists by idempotency key: {}", idempotencyKey);
        return repository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional
    public void updateTransferStatus(final Long transferId, final TransferStatus status) {
        log.debug("Updating transfer {} status to {}", transferId, status);
        final Transfer entity = repository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        entity.setStatus(status);
        repository.save(entity);
    }

    @Override
    @Transactional
    public void updateTransferStatusWithReason(
            final Long transferId,
            final TransferStatus status,
            final String reason) {
        log.debug("Updating transfer {} status to {} with reason: {}", transferId, status, reason);
        final Transfer entity = repository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        entity.setStatus(status);
        entity.setFailureReason(reason);
        repository.save(entity);
    }
}
