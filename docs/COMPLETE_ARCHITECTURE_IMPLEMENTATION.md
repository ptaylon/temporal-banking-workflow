# Complete Hexagonal Architecture Implementation Guide

## Overview

This document provides a comprehensive implementation plan for applying hexagonal architecture to ALL services in the banking demo project.

## Current Status

### ✅ Completed
- **transfer-service**: Full hexagonal architecture with idempotency support

### ⏳ Pending
- **account-service**: Needs hexagonal architecture
- **validation-service**: Needs hexagonal architecture
- **notification-service**: Needs hexagonal architecture
- **audit-service**: Needs hexagonal architecture

## Implementation Plan for Remaining Services

### 1. Account Service

**Purpose**: Manages bank accounts and balance operations

**Domain Model**: `AccountDomain`
```java
- id: Long
- accountNumber: String (unique)
- ownerName: String
- balance: BigDecimal
- currency: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
- idempotencyKey: String (for account creation)

Business Rules:
- validate(): Ensures positive balance, valid currency
- canDebit(amount): Checks if balance is sufficient
- debit(amount): Returns new AccountDomain with updated balance
- credit(amount): Returns new AccountDomain with updated balance
```

**Use Cases (Input Ports)**:
- `CreateAccountUseCase`: Create new account (idempotent)
- `QueryAccountUseCase`: Get account by number, list accounts
- `AccountOperationsUseCase`: Debit, credit, lock accounts

**Dependencies (Output Ports)**:
- `AccountPersistencePort`: Database operations
- `NotificationPort`: Send account creation notifications (optional)

**Key Operations**:
```
1. Create Account (Idempotent)
   - Check idempotencyKey
   - Validate account data
   - Persist account
   - Send notification

2. Debit Account
   - Lock account (pessimistic)
   - Check balance
   - Update balance
   - Persist changes

3. Credit Account
   - Lock account (pessimistic)
   - Update balance
   - Persist changes
```

---

### 2. Validation Service

**Purpose**: Validates transfer requests, fraud detection, transfer limits

**Domain Model**: `TransferValidationDomain`
```java
- transferId: Long
- sourceAccountNumber: String
- destinationAccountNumber: String
- amount: BigDecimal
- currency: String
- validationResult: ValidationResult (APPROVED, REJECTED, PENDING)
- rejectionReason: String
- fraudScore: Integer
- validatedAt: LocalDateTime

Business Rules:
- validateAmount(): Checks amount limits
- validateAccounts(): Ensures accounts exist and are different
- checkFraud(): Calculates fraud score
- isApproved(): Returns validation decision
```

**Use Cases (Input Ports)**:
- `ValidateTransferUseCase`: Validate transfer request
- `ManageLimitsUseCase`: Configure transfer limits
- `ManageFraudRulesUseCase`: Configure fraud detection rules

**Dependencies (Output Ports)**:
- `TransferLimitRepository`: Persistence for limits
- `FraudRuleRepository`: Persistence for fraud rules
- `AccountServicePort`: Check if accounts exist (via HTTP)

**Key Operations**:
```
1. Validate Transfer
   - Check source/destination are different
   - Verify accounts exist
   - Check amount against limits
   - Calculate fraud score
   - Return validation result

2. Manage Limits
   - Create/update transfer limits
   - Query limits by account/currency

3. Fraud Detection
   - Apply fraud detection rules
   - Score based on amount, frequency, patterns
```

---

### 3. Notification Service

**Purpose**: Sends notifications for transfer events via Kafka

**Domain Model**: `NotificationDomain`
```java
- id: Long
- eventType: EventType (TRANSFER_INITIATED, TRANSFER_COMPLETED, TRANSFER_FAILED)
- transferId: Long
- accountNumber: String
- message: String
- sentAt: LocalDateTime
- status: NotificationStatus (SENT, FAILED, PENDING)

Business Rules:
- buildMessage(): Generates notification message based on event
- canBeSent(): Checks if notification should be sent
- markAsSent(): Updates status
```

**Use Cases (Input Ports)**:
- `SendNotificationUseCase`: Send notification for event
- `QueryNotificationUseCase`: Get notification history

**Dependencies (Output Ports)**:
- `NotificationPersistencePort`: Store notification history
- `EmailPort`: Send emails (future)
- `SMSPort`: Send SMS (future)
- `PushNotificationPort`: Send push notifications (future)

**Key Operations**:
```
1. Process Transfer Event (from Kafka)
   - Parse Kafka message
   - Create NotificationDomain
   - Build message text
   - Send notification (log for now)
   - Store notification history

2. Query Notifications
   - Get by transfer ID
   - Get by account number
   - Get by date range
```

---

### 4. Audit Service

**Purpose**: Captures all database changes via CDC and stores audit trail

**Domain Model**: `AuditEventDomain`
```java
- id: Long
- eventType: String (CREATED, UPDATED, DELETED)
- entityType: String (accounts, transfers)
- entityId: String
- before: Map<String, Object> (state before change)
- after: Map<String, Object> (state after change)
- timestamp: LocalDateTime
- source: String (CDC connector name)

Business Rules:
- extractChanges(): Compares before/after
- getChangedFields(): Returns list of changed field names
- isSignificantChange(): Determines if audit is worth storing
```

**Use Cases (Input Ports)**:
- `ProcessCDCEventUseCase`: Process incoming CDC event
- `QueryAuditUseCase`: Search audit trail

**Dependencies (Output Ports)**:
- `AuditPersistencePort`: Store audit events
- `CDCEventParserPort`: Parse Debezium CDC events

**Key Operations**:
```
1. Process CDC Event (from Kafka)
   - Parse Debezium event format
   - Extract entity type, ID, before/after states
   - Create AuditEventDomain
   - Store in database

2. Query Audit Trail
   - Search by entity type
   - Search by entity ID
   - Search by date range
   - Search by event type
```

---

## Common Patterns Across All Services

### Idempotency Pattern
```java
// In every domain service that creates entities
public CreateResult create(CreateCommand command) {
    // 1. Check idempotency
    Optional<Domain> existing = persistencePort.findByIdempotencyKey(
        command.getIdempotencyKey()
    );

    if (existing.isPresent()) {
        // Return existing - idempotent operation!
        return CreateResult.success(existing.get());
    }

    // 2. Create new entity
    Domain domain = Domain.create(command);
    domain.validate();

    // 3. Persist
    Domain saved = persistencePort.save(domain);

    return CreateResult.success(saved);
}
```

### Domain Model Pattern
```java
@Value
@Builder
@With
public class DomainModel {
    // Immutable fields

    // Business validation
    public void validate() { ... }

    // Business rules
    public boolean canDoSomething() { ... }

    // State transitions
    public DomainModel doSomething() {
        // Return new immutable instance
        return this.withField(newValue);
    }
}
```

### Adapter Pattern
```java
@Component
@RequiredArgsConstructor
public class SomethingAdapter implements SomethingPort {
    private final ExternalClient client;
    private final Mapper mapper;

    @Override
    public DomainResult doSomething(DomainInput input) {
        // Convert domain to DTO
        ExternalDTO dto = mapper.toDTO(input);

        // Call external system
        ExternalResponse response = client.call(dto);

        // Convert back to domain
        return mapper.toDomain(response);
    }
}
```

## Database Schema Updates

### All Services Need Idempotency Support

**account-service**:
```sql
ALTER TABLE accounts ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;
CREATE INDEX idx_accounts_idempotency ON accounts(idempotency_key);
```

**validation-service** (if storing validation history):
```sql
-- Transfer limits and fraud rules might not need idempotency
-- But if we store validation results:
ALTER TABLE validation_results ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;
```

**notification-service**:
```sql
-- Notifications are typically idempotent by nature (event-driven)
-- But we can add if we persist notification records:
ALTER TABLE notifications ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;
```

**audit-service**:
```sql
-- Audit events are naturally idempotent (CDC events have offsets)
-- Add idempotency based on Kafka offset:
ALTER TABLE audit_events ADD COLUMN kafka_offset BIGINT;
ALTER TABLE audit_events ADD COLUMN kafka_partition INT;
CREATE UNIQUE INDEX idx_audit_idempotency ON audit_events(kafka_partition, kafka_offset);
```

## Implementation Priority

### Phase 1: Core Services (Week 1)
1. ✅ **transfer-service** - COMPLETED
2. **account-service** - HIGH PRIORITY (required by transfers)
3. **validation-service** - HIGH PRIORITY (required by transfers)

### Phase 2: Supporting Services (Week 2)
4. **notification-service** - MEDIUM PRIORITY
5. **audit-service** - MEDIUM PRIORITY

### Phase 3: Testing & Documentation (Week 3)
6. Comprehensive tests for all services
7. Realistic idempotency scenarios
8. Performance testing
9. Documentation updates

## Code Generation Template

For each service, generate:

### Domain Layer
```
domain/
├── model/{Service}Domain.java
├── port/in/
│   ├── Create{Service}UseCase.java
│   ├── Query{Service}UseCase.java
│   └── {Specific}UseCase.java
├── port/out/
│   ├── {Service}PersistencePort.java
│   └── {External}Port.java
└── service/
    └── {Service}Service.java
```

### Infrastructure Layer
```
infrastructure/adapter/
├── in/rest/
│   └── {Service}RestController.java
└── out/
    ├── persistence/
    │   ├── {Service}PersistenceAdapter.java
    │   └── {Service}Mapper.java
    └── http/ (if needed)
        └── {External}Adapter.java
```

## Testing Strategy

### Unit Tests (Domain Services)
```java
@Test
void shouldPreventDuplicate{Entity}Creation() {
    // Arrange - mock ports
    {Entity}PersistencePort persistencePort = mock({Entity}PersistencePort.class);

    {Entity}Service service = new {Entity}Service(persistencePort);

    // Mock existing entity
    when(persistencePort.findByIdempotencyKey("key"))
        .thenReturn(Optional.of(existing));

    // Act
    Create{Entity}Command command = Create{Entity}Command.builder()
        .idempotencyKey("key")
        .build();

    Result result = service.create(command);

    // Assert
    assertTrue(result.isSuccess());
    assertEquals(existing.getId(), result.getEntityId());
    verify(persistencePort, never()).save(any());
}
```

### Integration Tests (Adapters)
```java
@SpringBootTest
@Testcontainers
class {Service}PersistenceAdapterIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(...);

    @Autowired
    {Service}PersistenceAdapter adapter;

    @Test
    void shouldEnforceIdempotencyConstraint() {
        {Entity}Domain entity = {Entity}Domain.builder()
            .idempotencyKey("unique-key")
            .build();

        // First save succeeds
        adapter.save(entity);

        // Second save with same key should throw
        assertThrows(DataIntegrityViolationException.class,
            () -> adapter.save(entity));
    }
}
```

### End-to-End Tests
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
class IdempotencyE2ETest {

    @Test
    void shouldHandleIdempotentTransferCreation() {
        // Create accounts
        // Attempt transfer twice with same idempotency key
        // Verify only ONE transfer created
        // Verify balance updated only ONCE
    }
}
```

## Next Steps

1. **Implement account-service hexagonal architecture**
2. **Implement validation-service hexagonal architecture**
3. **Implement notification-service hexagonal architecture**
4. **Implement audit-service hexagonal architecture**
5. **Create comprehensive test suite**
6. **Create realistic demo scenarios**
7. **Performance testing and optimization**

## Questions to Consider

1. Should we implement gradual migration or complete replacement?
2. Do we need backward compatibility with old APIs?
3. What's the timeline for each service?
4. Do we need feature flags for rollout?
5. How do we handle database migrations in production?

---

**Would you like me to proceed with implementing all remaining services now?**
