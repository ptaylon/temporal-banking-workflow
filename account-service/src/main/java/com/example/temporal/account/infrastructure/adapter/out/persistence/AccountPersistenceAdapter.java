package com.example.temporal.account.infrastructure.adapter.out.persistence;

import com.example.temporal.account.domain.model.AccountDomain;
import com.example.temporal.account.domain.port.out.AccountPersistencePort;
import com.example.temporal.account.repository.AccountRepository;
import com.example.temporal.common.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter for account persistence using Spring Data JPA
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountRepository repository;
    private final AccountMapper mapper;

    @Override
    public AccountDomain save(final AccountDomain domain) {
        log.debug("Saving account: {}", domain.getAccountNumber());

        final Account entity = mapper.toEntity(domain);
        final Account saved = repository.save(entity);

        log.debug("Account saved with ID: {}", saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public AccountDomain update(final AccountDomain domain) {
        log.debug("Updating account: {}", domain.getAccountNumber());

        final Account entity = repository.findByAccountNumber(domain.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + domain.getAccountNumber()));

        mapper.updateEntity(entity, domain);
        final Account updated = repository.save(entity);

        log.debug("Account updated: {}", updated.getAccountNumber());
        return mapper.toDomain(updated);
    }

    @Override
    public Optional<AccountDomain> findByAccountNumber(final String accountNumber) {
        log.debug("Finding account by number: {}", accountNumber);
        return repository.findByAccountNumber(accountNumber)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountDomain> findByAccountNumberWithLock(final String accountNumber) {
        log.debug("Finding account by number with lock: {}", accountNumber);
        return repository.findByAccountNumberWithLock(accountNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AccountDomain> findByIdempotencyKey(final String idempotencyKey) {
        log.debug("Finding account by idempotency key: {}", idempotencyKey);
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AccountDomain> findById(final Long id) {
        log.debug("Finding account by ID: {}", id);
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<AccountDomain> findByAccountNumberIn(final List<String> accountNumbers) {
        log.debug("Finding accounts by numbers: {}", accountNumbers);
        return repository.findByAccountNumberIn(accountNumbers).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAccountNumber(final String accountNumber) {
        return repository.existsByAccountNumber(accountNumber);
    }

    @Override
    public boolean existsByIdempotencyKey(final String idempotencyKey) {
        return repository.existsByIdempotencyKey(idempotencyKey);
    }
}
