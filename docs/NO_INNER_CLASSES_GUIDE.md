# No Inner Classes Guide

This document describes the refactoring to eliminate inner classes from REST controllers.

## Principle: No Inner Classes

Inner classes (especially static nested classes used as DTOs) are a code smell because they:

1. **Hide dependencies** - Hard to discover and reuse
2. **Mix concerns** - Controller logic mixed with data structure definitions
3. **Poor organization** - Makes files longer and harder to navigate
4. **Harder to test** - Can't test DTOs independently
5. **No reusability** - Can't use the same DTO in multiple controllers
6. **Poor documentation** - Harder to generate API docs

## Before: Inner Classes

```java
@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {
    
    @PostMapping("/lock")
    public ResponseEntity<?> lockAccounts(@RequestBody LockAccountsRequest request) {
        // ...
    }
    
    // ❌ Inner class - hidden dependency
    @lombok.Data
    static class LockAccountsRequest {
        String sourceAccountNumber;
        String destinationAccountNumber;
    }
    
    // ❌ Another inner class
    @lombok.Data
    static class OperationRequest {
        BigDecimal amount;
    }
}
```

## After: Separate DTO Classes

```java
@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {
    
    private final AccountRestMapper accountRestMapper;
    
    @PostMapping("/lock")
    public ResponseEntity<MessageResponse> lockAccounts(
            @RequestBody final LockAccountsRequest request) {
        // ...
    }
}
```

**Separate files:**
```
account-service/
└── infrastructure/
    └── adapter/
        └── in/
            └── rest/
                ├── AccountRestController.java
                └── dto/
                    ├── LockAccountsRequest.java
                    ├── OperationRequest.java
                    ├── MessageResponse.java
                    └── BalanceResponse.java
```

## Created DTO Classes

### Account Service

| Class | Purpose | Package |
|-------|---------|---------|
| `LockAccountsRequest` | Request to lock multiple accounts | `...rest.dto` |
| `OperationRequest` | Request for debit/credit operations | `...rest.dto` |
| `BalanceResponse` | Response for balance queries | `...rest.dto` |
| `MessageResponse` | Simple message response | `...rest.dto` |
| `AccountCreateRequest` | Request to create account | `...rest.dto` |
| `AccountResponse` | Account details response | `...rest.dto` |

### Transfer Service

| Class | Purpose | Package |
|-------|---------|---------|
| `BatchCancelRequest` | Request for batch cancel | `...rest.dto` |

### Common Module

| Class | Purpose | Package |
|-------|---------|---------|
| `ErrorResponse` | Standard error response | `...common.dto` |
| `BatchOperationResponse` | Batch operation result | `...common.dto` |

## Benefits

### 1. **Discoverability**
```java
// ✅ Easy to find and import
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.LockAccountsRequest;

// ❌ Hidden inside controller
// static class LockAccountsRequest
```

### 2. **Reusability**
```java
// ✅ Can be used in multiple controllers
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountControllerV1 {
    @PostMapping("/lock")
    public ResponseEntity<MessageResponse> lockAccounts(
            @RequestBody final LockAccountsRequest request) {
        // Same DTO, different controller
    }
}
```

### 3. **Testability**
```java
// ✅ Can test DTO independently
@Test
void testLockAccountsRequest() {
    LockAccountsRequest request = new LockAccountsRequest();
    request.setSourceAccountNumber("123");
    // Test validation, serialization, etc.
}
```

### 4. **Documentation**
```java
/**
 * Request DTO for locking multiple accounts.
 * Used by AccountRestController.lockAccounts()
 */
@Data
public class LockAccountsRequest {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
}
```

### 5. **Organization**
```
Clear structure:
- Controllers handle HTTP
- DTOs carry data
- Mappers convert between them
- Services handle business logic
```

## Refactoring Examples

### Example 1: LockAccountsRequest

**Before:**
```java
@PostMapping("/lock")
public ResponseEntity<?> lockAccounts(@RequestBody LockAccountsRequest request) {
    operationsUseCase.lockAccounts(
        request.getSourceAccountNumber(),
        request.getDestinationAccountNumber());
    return ResponseEntity.ok(Map.of("message", "Accounts locked successfully"));
}

@lombok.Data
static class LockAccountsRequest {
    String sourceAccountNumber;
    String destinationAccountNumber;
}
```

**After:**
```java
// Separate file: dto/LockAccountsRequest.java
package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

@Data
public class LockAccountsRequest {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
}

// In controller
@PostMapping("/lock")
public ResponseEntity<MessageResponse> lockAccounts(
        @RequestBody final LockAccountsRequest request) {
    
    operationsUseCase.lockAccounts(
        request.getSourceAccountNumber(),
        request.getDestinationAccountNumber());
    
    final MessageResponse response = new MessageResponse();
    response.setMessage("Accounts locked successfully");
    return ResponseEntity.ok(response);
}
```

### Example 2: BalanceResponse

**Before:**
```java
@GetMapping("/{accountNumber}/balance")
public ResponseEntity<?> getBalance(@PathVariable String accountNumber) {
    BigDecimal balance = operationsUseCase.getBalance(accountNumber);
    return ResponseEntity.ok(Map.of("accountNumber", accountNumber, "balance", balance));
}
```

**After:**
```java
// Separate file: dto/BalanceResponse.java
package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

@Data
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal balance;
}

// In controller
@GetMapping("/{accountNumber}/balance")
public ResponseEntity<BalanceResponse> getBalance(
        @PathVariable final String accountNumber) {
    
    final BigDecimal balance = operationsUseCase.getBalance(accountNumber);
    
    final BalanceResponse response = new BalanceResponse();
    response.setAccountNumber(accountNumber);
    response.setBalance(balance);
    return ResponseEntity.ok(response);
}
```

## Package Structure

### Recommended Structure

```
account-service/
└── src/main/java/
    └── com/example/temporal/account/
        ├── infrastructure/
        │   └── adapter/
        │       └── in/
        │           └── rest/
        │               ├── AccountRestController.java
        │               ├── dto/
        │               │   ├── AccountCreateRequest.java
        │               │   ├── AccountResponse.java
        │               │   ├── LockAccountsRequest.java
        │               │   ├── OperationRequest.java
        │               │   ├── MessageResponse.java
        │               │   └── BalanceResponse.java
        │               └── mapper/
        │                   └── AccountRestMapper.java
        └── domain/
            ├── model/
            ├── port/
            └── service/
```

### Naming Conventions

1. **Request DTOs**: `{Action}{Entity}Request`
   - `LockAccountsRequest`
   - `CreateAccountRequest`
   - `ValidateTransferRequest`

2. **Response DTOs**: `{Entity}Response` or `{Action}Response`
   - `AccountResponse`
   - `BalanceResponse`
   - `MessageResponse`

3. **Package**: Always `...rest.dto` for REST DTOs

## Best Practices

### 1. One Class Per File
```java
// ✅ Good: One DTO per file
public class LockAccountsRequest { }

// ❌ Bad: Multiple classes in one file
public class LockAccountsRequest { }
class OperationRequest { }
```

### 2. Keep DTOs Simple
```java
// ✅ Good: Simple data carrier
@Data
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal balance;
}

// ❌ Bad: Business logic in DTO
@Data
public class BalanceResponse {
    private String accountNumber;
    private BigDecimal balance;
    
    public boolean isNegative() {  // ❌ Logic belongs in domain
        return balance.compareTo(BigDecimal.ZERO) < 0;
    }
}
```

### 3. Use Proper Packages
```java
// ✅ Good: Clear package structure
package com.example.temporal.account.infrastructure.adapter.in.rest.dto;

// ❌ Bad: All DTOs in controller
package com.example.temporal.account.infrastructure.adapter.in.rest;
```

### 4. Document DTOs
```java
/**
 * Request DTO for locking multiple accounts.
 * Used by AccountRestController.lockAccounts()
 */
@Data
public class LockAccountsRequest {
    /**
     * Source account number to lock
     */
    private String sourceAccountNumber;
    
    /**
     * Destination account number to lock
     */
    private String destinationAccountNumber;
}
```

## Migration Checklist

When refactoring inner classes:

- [ ] Create separate file for each inner class
- [ ] Move to appropriate package (`...rest.dto`)
- [ ] Add proper JavaDoc
- [ ] Update imports in controller
- [ ] Update imports in tests
- [ ] Update imports in mappers
- [ ] Verify build passes
- [ ] Verify tests pass
- [ ] Update API documentation

## Summary

| Aspect | Inner Classes | Separate Classes |
|--------|--------------|------------------|
| Discoverability | ❌ Hidden | ✅ Easy to find |
| Reusability | ❌ None | ✅ Full |
| Testability | ❌ Hard | ✅ Easy |
| Documentation | ❌ Poor | ✅ Auto-generated |
| Organization | ❌ Mixed | ✅ Clear |
| Maintenance | ❌ Hard | ✅ Easy |

---

**Version:** 1.0  
**Date:** March 15, 2026  
**Status:** ✅ Complete
