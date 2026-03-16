# Factory Methods and i18n Messages Guide

This guide demonstrates how to use factory methods for validation and internationalized messages throughout the codebase.

## Overview

The codebase now uses:
1. **Factory methods** for encapsulating complex validation logic
2. **i18n messages** for all user-facing text
3. **Final keywords** for immutability and safety
4. **Private helper methods** for better code organization

---

## 1. Using i18n Messages

### Before (Hardcoded Messages)
```java
throw new IllegalArgumentException("Insufficient funds in account: " + accountNumber);
```

### After (i18n Messages)
```java
throw new IllegalArgumentException(
    MessageResolver.resolveError(ErrorMessages.INSUFFICIENT_FUNDS, 
                                  balance, amount));
```

### Available Message Categories

#### Error Messages
```java
ErrorMessages.ACCOUNT_NOT_FOUND           // "Account not found: {0}"
ErrorMessages.INSUFFICIENT_FUNDS          // "Insufficient funds. Available: {0}, Required: {1}"
ErrorMessages.DUPLICATE_ENTRY             // "Duplicate entry: {0}"
ErrorMessages.ENTITY_NOT_FOUND            // "Entity not found: {0}"
ErrorMessages.VALIDATION_ERROR            // "Validation error: {0}"
ErrorMessages.OPERATION_FAILED            // "Operation failed: {0}"
```

#### Success Messages
```java
SuccessMessages.ACCOUNT_CREATED           // "Account created successfully: {0}"
SuccessMessages.TRANSFER_INITIATED        // "Transfer initiated successfully: {0}"
SuccessMessages.OPERATION_SUCCESSFUL      // "Operation completed successfully"
```

---

## 2. Factory Methods for Validation

### Example: Account Creation

#### Before (Scattered Validation)
```java
@Transactional
public CreateAccountResult createAccount(CreateAccountCommand command) {
    command.validate();
    
    String idempotencyKey = command.getIdempotencyKey() != null 
        ? command.getIdempotencyKey() 
        : UUID.randomUUID().toString();
    
    Optional<AccountDomain> existing = persistencePort.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return CreateAccountResult.success(existing.get());
    }
    
    if (persistencePort.existsByAccountNumber(command.getAccountNumber())) {
        return CreateAccountResult.error("Account number already exists: " + command.getAccountNumber());
    }
    
    AccountDomain account = AccountDomain.create(...);
    // ...
}
```

#### After (Factory Methods)
```java
@Transactional
public CreateAccountResult createAccount(final CreateAccountCommand command) {
    try {
        validateCreateAccountCommand(command);
        
        final String idempotencyKey = getOrGenerateIdempotencyKey(command.getIdempotencyKey());
        
        return checkIdempotencyAndCreate(idempotencyKey, command);
        
    } catch (final IllegalArgumentException e) {
        log.error("Validation error creating account: {}", e.getMessage());
        return CreateAccountResult.error(
            MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, e.getMessage()));
    }
}

private void validateCreateAccountCommand(final CreateAccountCommand command) {
    if (command == null) {
        throw new IllegalArgumentException(
            MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, "Command cannot be null"));
    }
    command.validate();
}

private String getOrGenerateIdempotencyKey(final String providedKey) {
    return providedKey != null ? providedKey : UUID.randomUUID().toString();
}

private CreateAccountResult checkIdempotencyAndCreate(
        final String idempotencyKey,
        final CreateAccountCommand command) {
    
    return persistencePort.findByIdempotencyKey(idempotencyKey)
            .map(existing -> handleExistingAccount(idempotencyKey, existing))
            .orElseGet(() -> createNewAccount(command, idempotencyKey));
}

private CreateAccountResult handleExistingAccount(
        final String idempotencyKey,
        final AccountDomain existing) {
    log.info("Account already exists for idempotency key: {}", idempotencyKey);
    return CreateAccountResult.success(existing);
}

private CreateAccountResult createNewAccount(
        final CreateAccountCommand command,
        final String idempotencyKey) {
    
    checkAccountNumberNotExists(command.getAccountNumber());
    
    final AccountDomain account = AccountDomain.create(...);
    final AccountDomain savedAccount = persistencePort.save(account);
    
    return CreateAccountResult.success(savedAccount);
}

private void checkAccountNumberNotExists(final String accountNumber) {
    if (persistencePort.existsByAccountNumber(accountNumber)) {
        throw new IllegalArgumentException(
            MessageResolver.resolveError(ErrorMessages.DUPLICATE_ENTRY, accountNumber));
    }
}
```

---

## 3. Private Helper Methods for Common Operations

### Finding Entities with Consistent Error Handling

#### Before
```java
AccountDomain account = persistencePort.findByAccountNumberWithLock(accountNumber)
    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
```

#### After
```java
final AccountDomain account = findAccountWithLock(
    accountNumber,
    ErrorMessages.ENTITY_NOT_FOUND
);

// Helper method
private AccountDomain findAccountWithLock(
        final String accountNumber,
        final String errorKey) {
    return persistencePort.findByAccountNumberWithLock(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                    MessageResolver.resolveError(errorKey, accountNumber)));
}
```

---

## 4. Complete Service Example

```java
@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final AccountPersistencePort persistencePort;

    @Override
    @Transactional
    public CreateAccountResult createAccount(final CreateAccountCommand command) {
        try {
            validateCreateAccountCommand(command);
            final String idempotencyKey = getOrGenerateIdempotencyKey(command.getIdempotencyKey());
            return checkIdempotencyAndCreate(idempotencyKey, command);
        } catch (final IllegalArgumentException e) {
            return CreateAccountResult.error(
                MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, e.getMessage()));
        }
    }

    @Override
    @Transactional
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        final AccountDomain account = findAccountWithLock(accountNumber, ErrorMessages.ENTITY_NOT_FOUND);
        final AccountDomain updatedAccount = account.debit(amount);
        persistencePort.update(updatedAccount);
        log.info("Account debited successfully: {} new balance: {}", 
                accountNumber, updatedAccount.getBalance());
    }

    @Override
    @Transactional
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        final AccountDomain account = findAccountWithLock(accountNumber, ErrorMessages.ENTITY_NOT_FOUND);
        final AccountDomain updatedAccount = account.credit(amount);
        persistencePort.update(updatedAccount);
        log.info("Account credited successfully: {} new balance: {}", 
                accountNumber, updatedAccount.getBalance());
    }

    // ========== FACTORY METHODS ==========

    private void validateCreateAccountCommand(final CreateAccountCommand command) {
        if (command == null) {
            throw new IllegalArgumentException(
                MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, "Command cannot be null"));
        }
        command.validate();
    }

    private String getOrGenerateIdempotencyKey(final String providedKey) {
        return providedKey != null ? providedKey : UUID.randomUUID().toString();
    }

    private CreateAccountResult checkIdempotencyAndCreate(
            final String idempotencyKey,
            final CreateAccountCommand command) {
        return persistencePort.findByIdempotencyKey(idempotencyKey)
                .map(existing -> handleExistingAccount(idempotencyKey, existing))
                .orElseGet(() -> createNewAccount(command, idempotencyKey));
    }

    private CreateAccountResult handleExistingAccount(
            final String idempotencyKey,
            final AccountDomain existing) {
        log.info("Account already exists for idempotency key: {}", idempotencyKey);
        return CreateAccountResult.success(existing);
    }

    private CreateAccountResult createNewAccount(
            final CreateAccountCommand command,
            final String idempotencyKey) {
        checkAccountNumberNotExists(command.getAccountNumber());
        final AccountDomain account = AccountDomain.create(
            command.getAccountNumber(),
            command.getOwnerName(),
            command.getInitialBalance(),
            command.getCurrency(),
            idempotencyKey
        );
        final AccountDomain savedAccount = persistencePort.save(account);
        return CreateAccountResult.success(savedAccount);
    }

    private void checkAccountNumberNotExists(final String accountNumber) {
        if (persistencePort.existsByAccountNumber(accountNumber)) {
            throw new IllegalArgumentException(
                MessageResolver.resolveError(ErrorMessages.DUPLICATE_ENTRY, accountNumber));
        }
    }

    private AccountDomain findAccountWithLock(final String accountNumber, final String errorKey) {
        return persistencePort.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageResolver.resolveError(errorKey, accountNumber)));
    }
}
```

---

## 5. Benefits

### Readability
- Method names describe intent (`checkIdempotencyAndCreate`, `findAccountWithLock`)
- Business logic is separated from error handling
- Each method has a single responsibility

### Maintainability
- Error messages are centralized
- Validation logic is reusable
- Changes to error handling affect only helper methods

### Testability
- Private methods can be tested indirectly through public API
- Factory methods can be extracted to separate classes if needed
- Mocking is easier with well-defined boundaries

### Consistency
- All errors use the same format
- All validation follows the same pattern
- Code style is uniform across services

---

## 6. Best Practices

### 1. Always Use Final for Parameters
```java
private AccountDomain findAccountWithLock(
        final String accountNumber,  // ✅ final
        final String errorKey) {     // ✅ final
```

### 2. Use i18n for All User Messages
```java
// ✅ Good
throw new IllegalArgumentException(
    MessageResolver.resolveError(ErrorMessages.ENTITY_NOT_FOUND, accountNumber));

// ❌ Bad
throw new IllegalArgumentException("Account not found: " + accountNumber);
```

### 3. Extract Complex Logic to Factory Methods
```java
// ✅ Good - Complex logic extracted
public CreateAccountResult createAccount(final CreateAccountCommand command) {
    validateCreateAccountCommand(command);
    final String idempotencyKey = getOrGenerateIdempotencyKey(command.getIdempotencyKey());
    return checkIdempotencyAndCreate(idempotencyKey, command);
}

// ❌ Bad - Everything in one method
public CreateAccountResult createAccount(final CreateAccountCommand command) {
    // 50 lines of mixed validation, business logic, and error handling
}
```

### 4. Use Descriptive Method Names
```java
// ✅ Good
checkIdempotencyAndCreate(...)
handleExistingAccount(...)
createNewAccount(...)
checkAccountNumberNotExists(...)

// ❌ Vague
process(...)
handle(...)
doCreate(...)
```

### 5. Document Factory Methods
```java
/**
 * Checks for existing account by idempotency key and creates if not exists.
 *
 * @param idempotencyKey the idempotency key
 * @param command the create command
 * @return the creation result
 */
private CreateAccountResult checkIdempotencyAndCreate(
        final String idempotencyKey,
        final CreateAccountCommand command) {
```

---

## 7. Message Properties Format

### English (messages_en.properties)
```properties
error.account.insufficient-funds=Insufficient funds. Available: {0}, Required: {1}
error.general.entity-not-found=Entity not found: {0}
error.general.duplicate-entry=Duplicate entry: {0}
```

### Portuguese (messages_pt_BR.properties)
```properties
error.account.insufficient-funds=Saldo insuficiente. Disponível: {0}, Necessário: {1}
error.general.entity-not-found=Entidade não encontrada: {0}
error.general.duplicate-entry=Entrada duplicada: {0}
```

### Using Placeholders
```java
// Multiple placeholders
MessageResolver.resolveError(ErrorMessages.INSUFFICIENT_FUNDS, balance, amount);

// Single placeholder
MessageResolver.resolveError(ErrorMessages.ENTITY_NOT_FOUND, accountNumber);

// No placeholder
MessageResolver.resolveError(ErrorMessages.OPERATION_SUCCESSFUL);
```

---

**Version:** 1.0
**Date:** March 15, 2026
**Status:** ✅ Implemented
