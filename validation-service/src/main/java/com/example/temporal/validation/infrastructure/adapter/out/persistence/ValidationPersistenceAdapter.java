package com.example.temporal.validation.infrastructure.adapter.out.persistence;

import com.example.temporal.validation.domain.model.TransferValidationDomain;
import com.example.temporal.validation.domain.port.out.ValidationPersistencePort;
import com.example.temporal.validation.entity.TransferValidationEntity;
import com.example.temporal.validation.repository.TransferValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence adapter for validation
 * Implements the ValidationPersistencePort using JPA
 */
@Component
@RequiredArgsConstructor
public class ValidationPersistenceAdapter implements ValidationPersistencePort {

    private final TransferValidationRepository repository;
    private final ValidationMapper mapper;

    @Override
    public TransferValidationDomain save(final TransferValidationDomain domain) {
        final var entity = mapper.toEntity(domain);
        final var savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<TransferValidationDomain> findById(final Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TransferValidationDomain> findByIdempotencyKey(
            final String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<TransferValidationDomain> findByTransferId(final String transferId) {
        return repository.findByTransferId(transferId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferValidationDomain> findByAccountNumber(final String accountNumber) {
        return repository.findBySourceAccountNumberOrDestinationAccountNumber(
                accountNumber, accountNumber)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferValidationDomain> findPendingValidations() {
        return repository.findByValidationResult(
                TransferValidationEntity.ValidationResult.PENDING)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
