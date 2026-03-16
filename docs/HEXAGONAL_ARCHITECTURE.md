# Hexagonal Architecture Implementation

## Overview

This document describes the hexagonal architecture (ports and adapters pattern) implementation for the transfer-service. The goal is to create a clean, maintainable, and testable codebase that isolates business logic from infrastructure concerns.

## Architecture Structure

```
transfer-service/src/main/java/com/example/temporal/transfer/
├── domain/                          # Core business logic (framework-independent)
│   ├── model/
│   │   └── TransferDomain.java     # Pure domain model with business rules
│   ├── port/
│   │   ├── in/                     # Use cases (driving ports)
│   │   │   ├── InitiateTransferUseCase.java
│   │   │   ├── QueryTransferUseCase.java
│   │   │   └── ControlTransferUseCase.java
│   │   └── out/                    # Dependencies (driven ports)
│   │       ├── TransferPersistencePort.java
│   │       ├── WorkflowOrchestrationPort.java
│   │       ├── AccountPort.java
│   │       ├── ValidationPort.java
│   │       └── NotificationPort.java
│   └── service/                    # Domain services implementing use cases
│       ├── TransferService.java
│       └── TransferControlService.java
│
├── infrastructure/                  # Framework-specific implementations
│   └── adapter/
│       ├── in/                     # Driving adapters (external → domain)
│       │   └── rest/               # REST controllers
│       └── out/                    # Driven adapters (domain → external)
│           ├── persistence/        # JPA/Database
│           │   ├── TransferPersistenceAdapter.java
│           │   └── TransferMapper.java
│           ├── http/               # Feign clients
│           └── messaging/          # Kafka producers
│
└── application/                     # Application layer (orchestration)
    ├── workflow/                    # Temporal workflows
    └── activity/                    # Temporal activities
```

## Key Components

### 1. Domain Layer

#### TransferDomain.java
**Location**: `domain/model/TransferDomain.java`

- **Pure domain model** - no framework dependencies (no JPA, no Spring)
- **Immutable** - uses Lombok's `@Value` and `@With`
- **Self-validating** - contains business validation logic
- **Business rules** - methods like `canBeCancelled()`, `canBePaused()`, `isInFinalState()`
- **Idempotency support** - includes `idempotencyKey` field

```java
@Value
@Builder
@With
public class TransferDomain {
    Long id;
    String sourceAccountNumber;
    String destinationAccountNumber;
    BigDecimal amount;
    String currency;
    TransferStatus status;
    String failureReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String idempotencyKey;

    // Business validation and rules
    public void validate() { ... }
    public boolean canBeCancelled() { ... }
    public boolean canBePaused() { ... }
}
```

### 2. Ports (Interfaces)

#### Input Ports (Driving Ports)
Define **what** the application can do (use cases):

- **InitiateTransferUseCase** - Create new transfers
- **QueryTransferUseCase** - Query transfer status and history
- **ControlTransferUseCase** - Pause, resume, cancel transfers

#### Output Ports (Driven Ports)
Define **dependencies** the domain needs:

- **TransferPersistencePort** - Database operations
- **WorkflowOrchestrationPort** - Temporal workflow management
- **AccountPort** - Account service operations
- **ValidationPort** - Validation service operations
- **NotificationPort** - Notification sending

### 3. Domain Services

#### TransferService.java
**Location**: `domain/service/TransferService.java`

- Implements `InitiateTransferUseCase` and `QueryTransferUseCase`
- **Pure business logic** - no HTTP, no database, no framework code
- Depends only on **ports** (interfaces)
- **Testable** - can be tested with mocks

**Key Features**:
- ✅ Idempotency support using `idempotencyKey`
- ✅ Checks for duplicate requests before creating new transfer
- ✅ Validates domain rules before persistence
- ✅ Orchestrates workflow initiation

#### TransferControlService.java
**Location**: `domain/service/TransferControlService.java`

- Implements `ControlTransferUseCase`
- Controls workflow lifecycle (pause, resume, cancel)
- Validates business rules before control actions

### 4. Infrastructure Adapters

#### TransferPersistenceAdapter.java
**Location**: `infrastructure/adapter/out/persistence/TransferPersistenceAdapter.java`

- **Implements** `TransferPersistencePort`
- Adapts domain to JPA/Spring Data
- Uses `TransferMapper` to convert between domain and JPA entities
- Handles all database operations

#### TransferMapper.java
**Location**: `infrastructure/adapter/out/persistence/TransferMapper.java`

- Maps between `TransferDomain` (domain) and `Transfer` (JPA entity)
- **Isolates** domain from persistence concerns
- Handles bidirectional conversion

## Benefits of This Architecture

### 1. **Separation of Concerns**
- Business logic in domain layer (pure Java)
- Infrastructure code in adapters (Spring, JPA, Temporal)
- Clear boundaries between layers

### 2. **Testability**
- Domain services can be tested without Spring, database, or Temporal
- Mock ports for fast unit tests
- Integration tests focus on adapters

### 3. **Flexibility**
- Easy to swap implementations (e.g., switch from JPA to MongoDB)
- Framework changes don't affect business logic
- Can run domain logic outside of Spring context

### 4. **Maintainability**
- Clear structure - easy to find code
- Single Responsibility Principle
- Dependencies point inward (domain doesn't depend on infrastructure)

### 5. **Idempotency Built-in**
- Idempotency is a first-class citizen in the domain
- Automatic handling of duplicate requests
- Database-level uniqueness constraint

## Idempotency Implementation

### Database Level
```java
// Transfer.java (JPA Entity)
@Column(unique = true, length = 100)
private String idempotencyKey;
```

### Repository Level
```java
// TransferRepository.java
Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
boolean existsByIdempotencyKey(String idempotencyKey);
```

### Domain Level
```java
// TransferService.java
public TransferInitiationResult initiateTransfer(InitiateTransferCommand command) {
    // Check for duplicate request
    Optional<TransferDomain> existingTransfer =
        persistencePort.findByIdempotencyKey(idempotencyKey);

    if (existingTransfer.isPresent()) {
        // Return existing transfer (idempotent operation)
        return TransferInitiationResult.success(...);
    }

    // Create new transfer
    ...
}
```

## Migration Path

### Phase 1: ✅ Completed
- [x] Created domain models and ports
- [x] Implemented domain services
- [x] Created persistence adapter
- [x] Added idempotency support

### Phase 2: Next Steps
- [ ] Create remaining infrastructure adapters:
  - [ ] WorkflowOrchestrationAdapter (Temporal)
  - [ ] AccountAdapter (Feign client wrapper)
  - [ ] ValidationAdapter (Feign client wrapper)
  - [ ] NotificationAdapter (Kafka wrapper)
- [ ] Refactor controllers to use domain services
- [ ] Refactor activities to use ports
- [ ] Update tests

### Phase 3: Future
- [ ] Add comprehensive unit tests for domain services
- [ ] Add integration tests for adapters
- [ ] Add end-to-end tests
- [ ] Create idempotency test scenarios

## Usage Examples

### Creating a Transfer (Idempotent)

```java
// Client sends same request twice (with same idempotency key)
InitiateTransferCommand command = InitiateTransferCommand.builder()
    .sourceAccountNumber("123456")
    .destinationAccountNumber("789012")
    .amount(new BigDecimal("100.00"))
    .currency("BRL")
    .idempotencyKey("unique-key-123") // Same key for both requests
    .build();

// First request - creates new transfer
TransferInitiationResult result1 = transferService.initiateTransfer(command);
// Returns: transferId=1, workflowId=transfer-1

// Second request (duplicate) - returns existing transfer
TransferInitiationResult result2 = transferService.initiateTransfer(command);
// Returns: transferId=1, workflowId=transfer-1 (same as first)
```

### Testing Domain Service

```java
@Test
void testInitiateTransfer() {
    // Arrange - mock ports
    TransferPersistencePort persistencePort = mock(TransferPersistencePort.class);
    WorkflowOrchestrationPort orchestrationPort = mock(WorkflowOrchestrationPort.class);

    TransferService service = new TransferService(persistencePort, orchestrationPort);

    // Act
    TransferInitiationResult result = service.initiateTransfer(command);

    // Assert
    assertEquals("INITIATED", result.getStatus());
    verify(persistencePort).save(any());
    verify(orchestrationPort).startTransferWorkflow(any(), any());
}
```

## Best Practices

1. **Keep domain pure** - No framework annotations in domain layer
2. **Use interfaces for ports** - Enable easy mocking and swapping
3. **Map at boundaries** - Convert between domain and infrastructure models
4. **Validate in domain** - Business rules belong in domain services
5. **Test domain without infrastructure** - Fast, reliable unit tests

## References

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Ports and Adapters Pattern](https://herbertograca.com/2017/09/14/ports-adapters-architecture/)
