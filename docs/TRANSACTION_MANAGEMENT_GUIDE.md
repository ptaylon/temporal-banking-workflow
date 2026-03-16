# Transaction Management Best Practices

This document describes the transaction management strategy adopted in the Temporal Banking Workflow project.

## Overview

We follow the principle: **`@Transactional` only at the service layer**, not on simple CRUD operations in persistence adapters.

---

## Why Remove `@Transactional` from Persistence Adapters?

### 1. **Spring Data JPA Already Handles Transactions**
Spring Data JPA repositories automatically participate in existing transactions. Each repository method (`save`, `findById`, `delete`, etc.) is already transactional.

### 2. **Unnecessary Overhead**
Adding `@Transactional` on every persistence method creates:
- Extra transaction management overhead
- Redundant transaction boundaries
- Confusing transaction propagation

### 3. **Service Layer Should Control Transactions**
Business operations that span multiple repository calls should be transactional at the **service layer**, not the persistence layer.

---

## Transaction Strategy

### ✅ **Correct: Service Layer Transaction**

```java
@Service
public class AccountService {
    
    @Override
    @Transactional  // ✅ Transaction at service layer
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        // Multiple operations in one transaction
        final AccountDomain account = findAccountWithLock(accountNumber);
        final AccountDomain updated = account.debit(amount);
        persistencePort.update(updated);
    }
}
```

### ❌ **Incorrect: Persistence Layer Transaction**

```java
@Component
public class AccountPersistenceAdapter {
    
    @Override
    @Transactional  // ❌ Unnecessary - repository already transactional
    public AccountDomain save(final AccountDomain domain) {
        final Account entity = mapper.toEntity(domain);
        final Account saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    @Transactional  // ❌ Unnecessary - simple read operation
    public Optional<AccountDomain> findById(final Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
```

---

## When to Use `@Transactional` in Persistence Adapters

### 1. **Write Operations (Optional)**
For `save()` operations, `@Transactional` is optional but can be omitted since:
- Spring Data JPA's `save()` is already transactional
- The service layer should control transaction boundaries

```java
// ✅ Good - No @Transactional needed
@Override
public AccountDomain save(final AccountDomain domain) {
    final Account entity = mapper.toEntity(domain);
    return mapper.toDomain(repository.save(entity));
}
```

### 2. **Update Operations with Multiple Steps**
When an update method performs multiple operations that must be atomic:

```java
// ✅ Acceptable - Complex update with multiple steps
@Override
@Transactional
public AccountDomain update(final AccountDomain domain) {
    final Account entity = repository.findByAccountNumber(domain.getAccountNumber())
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    
    mapper.updateEntity(entity, domain);  // Step 1: Update entity
    final Account updated = repository.save(entity);  // Step 2: Save
    
    return mapper.toDomain(updated);
}
```

### 3. **Pessimistic Locking**
When using pessimistic locking, keep `@Transactional(readOnly = true)`:

```java
// ✅ Good - Pessimistic lock requires transaction
@Override
@Transactional(readOnly = true)
public Optional<AccountDomain> findByAccountNumberWithLock(final String accountNumber) {
    return repository.findByAccountNumberWithLock(accountNumber)
            .map(mapper::toDomain);
}
```

---

## Refactored Code Examples

### AccountPersistenceAdapter

#### Before
```java
@Component
public class AccountPersistenceAdapter {
    
    @Override
    @Transactional  // ❌ Removed
    public AccountDomain save(AccountDomain domain) { }
    
    @Override
    @Transactional  // ❌ Removed
    public Optional<AccountDomain> findByAccountNumber(String accountNumber) { }
    
    @Override
    @Transactional  // ❌ Removed
    public Optional<AccountDomain> findById(Long id) { }
}
```

#### After
```java
@Component
public class AccountPersistenceAdapter {
    
    @Override
    public AccountDomain save(final AccountDomain domain) { }  // ✅ No @Transactional
    
    @Override
    public Optional<AccountDomain> findByAccountNumber(final String accountNumber) { }  // ✅ No @Transactional
    
    @Override
    public Optional<AccountDomain> findById(final Long id) { }  // ✅ No @Transactional
    
    @Override
    @Transactional(readOnly = true)  // ✅ Kept for pessimistic lock
    public Optional<AccountDomain> findByAccountNumberWithLock(final String accountNumber) { }
}
```

---

## Files Refactored

### Persistence Adapters (Removed `@Transactional`)

| File | Methods Changed |
|------|----------------|
| `AccountPersistenceAdapter` | `save()`, `findByAccountNumber()`, `findById()`, `findByIdempotencyKey()`, `findByAccountNumberIn()`, `existsBy*()` |
| `TransferPersistenceAdapter` | `save()`, `findById()`, `findByIdempotencyKey()`, `findByAccountNumber()`, `findByStatus()`, `existsByIdempotencyKey()` |
| `NotificationPersistenceAdapter` | `save()`, `findById()`, `findByIdempotencyKey()`, `findByTransferId()`, `findByAccountNumber()`, `findByEventType()`, `findByStatus()` |
| `AuditPersistenceAdapter` | `save()`, `findById()`, `findByIdempotencyKey()`, `findByEntityTypeAndEntityId()`, `findByEntityType()`, `findByEventType()`, `findByUserId()`, `findByEntityTypeAndEventTypesInRange()` |
| `ValidationPersistenceAdapter` | `save()`, `findById()`, `findByIdempotencyKey()`, `findByTransferId()`, `findByAccountNumber()`, `findPendingValidations()` |

### Methods That Keep `@Transactional`

| Method | Reason |
|--------|--------|
| `AccountPersistenceAdapter.update()` | Multiple operations (find + update + save) |
| `TransferPersistenceAdapter.update()` | Multiple operations (find + update + save) |
| `TransferPersistenceAdapter.updateTransferStatus()` | Write operation with find + update |
| `TransferPersistenceAdapter.updateTransferStatusWithReason()` | Write operation with find + update |
| `AccountPersistenceAdapter.findByAccountNumberWithLock()` | Pessimistic locking requires transaction |

---

## Benefits

### 1. **Cleaner Code**
- Less annotation clutter
- Clearer separation of concerns
- Easier to understand transaction boundaries

### 2. **Better Performance**
- No unnecessary transaction creation
- Reduced transaction management overhead
- Let Spring optimize automatically

### 3. **Correct Transaction Boundaries**
- Transactions controlled at business logic layer
- Multiple operations can share a single transaction
- Easier to reason about atomicity

### 4. **Follows Spring Best Practices**
- Aligns with Spring Data JPA design
- Matches Spring Boot auto-configuration behavior
- Recommended by Spring team

---

## Transaction Propagation

### Service Layer Controls Everything

```java
@Service
public class AccountService {
    
    @Transactional  // ✅ Single transaction for entire operation
    public void transferMoney(
            final String fromAccount,
            final String toAccount,
            final BigDecimal amount) {
        
        // All these operations share the same transaction
        final AccountDomain source = accountPersistenceAdapter.findByAccountNumberWithLock(fromAccount);
        final AccountDomain destination = accountPersistenceAdapter.findByAccountNumberWithLock(toAccount);
        
        source.debit(amount);
        destination.credit(amount);
        
        accountPersistenceAdapter.update(source);
        accountPersistenceAdapter.update(destination);
    }
}
```

### Repository Methods Join Existing Transaction

```java
@Component
public class AccountPersistenceAdapter {
    
    // No @Transactional - joins existing transaction from service
    public AccountDomain save(final AccountDomain domain) {
        return mapper.toDomain(repository.save(mapper.toEntity(domain)));
    }
    
    // No @Transactional - read-only operation
    public Optional<AccountDomain> findById(final Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
```

---

## Testing Considerations

### Integration Tests

Tests still work correctly because:
1. Spring Test creates transactions for test methods
2. Repository methods participate in test transactions
3. Rollback works as expected

```java
@SpringBootTest
@Transactional  // Test transaction
class AccountServiceTest {
    
    @Test
    void shouldDebitAccount() {
        // Test runs in transaction, rolls back after
        accountService.debitAccount("123", new BigDecimal("100"));
    }
}
```

---

## Common Mistakes to Avoid

### ❌ Don't Add `@Transactional` Everywhere

```java
// ❌ Bad - Unnecessary transaction overhead
@Component
public class AccountPersistenceAdapter {
    
    @Transactional  // ❌ Not needed
    public Optional<AccountDomain> findById(Long id) { }
    
    @Transactional  // ❌ Not needed
    public List<AccountDomain> findAll() { }
    
    @Transactional  // ❌ Not needed
    public boolean existsById(Long id) { }
}
```

### ✅ Do Use at Service Layer

```java
// ✅ Good - Transaction at business boundary
@Service
public class AccountService {
    
    @Transactional  // ✅ Controls business operation
    public void debitAccount(String accountNumber, BigDecimal amount) {
        // Business logic with multiple operations
    }
}
```

---

## Summary

| Layer | `@Transactional`? | Reason |
|-------|------------------|--------|
| **Service** | ✅ Yes | Business operations, multiple steps |
| **Persistence (Read)** | ❌ No | Spring Data JPA handles it |
| **Persistence (Write)** | ❌ No (usually) | Let service layer control |
| **Persistence (Update)** | ⚠️ Optional | If multiple operations |
| **Persistence (Lock)** | ✅ Yes (readOnly) | Pessimistic locking needs transaction |

---

**Version:** 1.0  
**Date:** March 15, 2026  
**Status:** ✅ Implemented
