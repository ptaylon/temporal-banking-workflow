# Banking Demo with Temporal.io - Version 2.0

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Tests](https://img.shields.io/badge/tests-31%20passing-brightgreen)]()
[![Java](https://img.shields.io/badge/java-21-blue)]()
[![Temporal](https://img.shields.io/badge/temporal-1.24.1-blue)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Complete banking system demonstrating Temporal.io usage for money transfer orchestration with microservices architecture, implementing distributed saga patterns, Change Data Capture (CDC), and real-time comprehensive auditing.

## 🎯 What's New in Version 2.0

### ✨ Major Features

#### 1. **Hexagonal Architecture** (100% Complete)
All 5 microservices now follow hexagonal architecture principles:
- **Clean separation** between domain logic and infrastructure
- **Testable** domain services without Spring/Database dependencies
- **Flexible** - easy to swap implementations (database, messaging, etc.)
- **Idempotency** built into all services

#### 2. **Advanced Temporal Features**

##### Search Attributes 🔍
Query and monitor workflows with custom attributes:
- `TransferAmount` - Filter by transfer value
- `SourceAccount` / `DestinationAccount` - Track by account
- `Currency` - Multi-currency support
- `TransferStatus` - Real-time status tracking
- `Priority` - Auto-calculated priority (0-5) based on amount

**Priority Levels:**
```
Priority 5: ≥ $100,000 (Highest - VIP transfers)
Priority 4: ≥ $50,000  (High value)
Priority 3: ≥ $10,000  (Medium-high)
Priority 2: ≥ $1,000   (Standard)
Priority 1: ≥ $100     (Small)
Priority 0: < $100     (Micro transfers)
```

##### Configurable Timers ⏱️
Schedule transfers with flexible delays:
- **Delay before execution** - Schedule for later
- **Timeout configuration** - Set maximum execution time
- **Cancellation support** - Cancel during delay period
- **Non-cancellable mode** - For critical transfers

##### Signals & Queries 📡
Interactive workflow control:
- **Pause** - Temporarily halt execution
- **Resume** - Continue paused workflows
- **Cancel** - Terminate with reason tracking
- **Status queries** - Real-time workflow state

#### 3. **Enhanced Observability**
- Advanced search API with 6 endpoints
- Real-time workflow monitoring
- Comprehensive audit trail via CDC
- Structured logging throughout

---

## 🏗️ System Architecture

### Microservices

| Service | Port | Responsibility | Architecture |
|---------|------|----------------|--------------|
| **Account Service** | 8081 | Account management, balance operations | Hexagonal ✅ |
| **Transfer Service** | 8082 | Transfer orchestration, Temporal workflows | Hexagonal ✅ |
| **Validation Service** | 8087 | Transfer validation, fraud detection | Hexagonal ✅ |
| **Notification Service** | 8086 | Status notifications via Kafka | Hexagonal ✅ |
| **Audit Service** | 8085 | CDC audit trail, event tracking | Hexagonal ✅ |

### Infrastructure Components

| Component | Port | Purpose |
|-----------|------|---------|
| **PostgreSQL (Main)** | 5432 | Primary database with WAL for CDC |
| **PostgreSQL (Audit)** | 5433 | Audit event storage (JSONB) |
| **Temporal Server** | 7233 | Workflow orchestration engine |
| **Temporal UI** | 8088 | Workflow monitoring dashboard |
| **Kafka** | 9092 | Event streaming (CDC + notifications) |
| **Kafka UI** | 8090 | Kafka topic visualization |
| **Debezium** | 8083 | CDC connector for PostgreSQL |
| **OpenSearch** | 9200 | Temporal workflow persistence |

---

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (tested with OpenJDK 21)
- **Maven 3.8+** (or use included `./mvnw`)
- **Docker & Docker Compose** (v20.10+)
- **Make** (optional but recommended)

### One-Command Setup

```bash
# Clone and setup
git clone <repository-url>
cd temporal-banking-workflow

# Complete setup (first time)
make setup

# Start development environment
make -f Makefile.dev dev-start

# Verify everything works
make debug-all
```

### Manual Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Wait for services (30 seconds)
sleep 30

# 3. Configure CDC
make setup-cdc

# 4. Build all services
./mvnw clean package -DskipTests

# 5. Start services (each in separate terminal)
java -jar account-service/target/account-service-1.0-SNAPSHOT.jar
java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar
java -jar validation-service/target/validation-service-1.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-1.0-SNAPSHOT.jar
java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar
```

---

## 📋 API Usage Guide

### 1. Create Accounts

```bash
# Source account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "123456",
    "ownerName": "John Silva",
    "balance": 1000.00,
    "currency": "BRL"
  }'

# Destination account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "789012",
    "ownerName": "Maria Santos",
    "balance": 500.00,
    "currency": "BRL"
  }'
```

### 2. Initiate Transfer

#### Basic Transfer
```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "123456",
    "destinationAccountNumber": "789012",
    "amount": 100.00,
    "currency": "BRL"
  }'
```

#### Transfer with Delay (NEW!)
```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "123456",
    "destinationAccountNumber": "789012",
    "amount": 500.00,
    "currency": "BRL",
    "delayInSeconds": 30,
    "allowCancelDuringDelay": true
  }'
```

#### Idempotent Transfer (NEW!)
```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-123" \
  -d '{
    "sourceAccountNumber": "123456",
    "destinationAccountNumber": "789012",
    "amount": 100.00,
    "currency": "BRL"
  }'
```

### 3. Control Workflow Execution (NEW!)

#### Pause Transfer
```bash
curl -X POST http://localhost:8082/api/transfers/{workflowId}/pause
```

#### Resume Transfer
```bash
curl -X POST http://localhost:8082/api/transfers/{workflowId}/resume
```

#### Cancel Transfer
```bash
curl -X POST http://localhost:8082/api/transfers/{workflowId}/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer requested cancellation"
  }'
```

#### Get Control Status
```bash
curl http://localhost:8082/api/transfers/{workflowId}/control-status
```

### 4. Search Transfers (NEW!)

#### Search by Account
```bash
curl http://localhost:8082/api/transfers/search/by-account/123456
```

#### Search by Amount Range
```bash
curl "http://localhost:8082/api/transfers/search/by-amount-range?minAmount=100&maxAmount=1000"
```

#### Get High Priority Transfers
```bash
curl http://localhost:8082/api/transfers/search/high-priority
```

#### Get Analytics Summary
```bash
curl http://localhost:8082/api/transfers/search/analytics/summary
```

**Example Response:**
```json
{
  "totalTransfers": 150,
  "completedTransfers": 145,
  "failedTransfers": 3,
  "runningTransfers": 2,
  "totalAmount": 125000.00,
  "averageAmount": 833.33
}
```

### 5. Query Audit Trail

#### By Account
```bash
curl http://localhost:8085/api/audit/accounts/123456
```

#### By Date Range
```bash
curl "http://localhost:8085/api/audit/search?entityType=accounts&eventTypes=ACCOUNTS_CREATED,ACCOUNTS_UPDATED&start=2024-01-01T00:00:00&end=2024-12-31T23:59:59"
```

---

## 🏛️ Technical Architecture

### Hexagonal Architecture Structure

```
┌─────────────────────────────────────────────────────────┐
│              INFRASTRUCTURE LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ REST Adapter │  │ Kafka Adapter│  │ JPA Adapter  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │          │
│         └─────────────────┼─────────────────┘          │
│                           │                             │
│                           ▼                             │
│  ╔═══════════════════════════════════════════════════╗ │
│  ║              DOMAIN LAYER                          ║ │
│  ║  ┌─────────────────────────────────────────┐     ║ │
│  ║  │  Domain Models (Immutable)              │     ║ │
│  ║  │  Domain Services (Business Logic)       │     ║ │
│  ║  │  Use Case Interfaces (Ports)            │     ║ │
│  ║  └─────────────────────────────────────────┘     ║ │
│  ╚═══════════════════════════════════════════════════╝ │
└─────────────────────────────────────────────────────────┘
```

### Saga Pattern Implementation

```
┌─────────────────────────────────────────────────────────┐
│              MoneyTransferWorkflow                       │
│                                                          │
│  1. Validate Transfer ─────────────┐                    │
│     ├─ Check accounts              │                    │
│     ├─ Check limits                │ Retry Policy       │
│     └─ Fraud detection             │ - Initial: 2s      │
│                                     │ - Max: 5min       │
│  2. Lock Accounts ─────────────────┤ - Attempts: 20    │
│     ├─ Source account              │                    │
│     └─ Destination account         │                    │
│                                     │                    │
│  3. Debit Source ──────────────────┤                    │
│     └─ Update balance              │                    │
│                                     │                    │
│  4. Credit Destination ────────────┤                    │
│     └─ Update balance              │                    │
│                                     │                    │
│  5. Complete ──────────────────────┘                    │
│     ├─ Update status                                     │
│     └─ Send notification                                 │
│                                                          │
│  Compensation (on failure):                              │
│  5→4→3→2→1 (reverse order with compensating actions)    │
└─────────────────────────────────────────────────────────┘
```

### Change Data Capture (CDC) Flow

```
┌──────────────┐    WAL     ┌──────────────┐
│ PostgreSQL   │ ─────────▶ │ Debezium     │
│ (banking)    │  Logical   │ Connector    │
│              │  Replication│              │
└──────────────┘            └──────┬───────┘
                                   │
                                   ▼
                          ┌────────────────┐
                          │ Kafka Topics   │
                          │ - banking.     │
                          │   public.      │
                          │   accounts     │
                          │ - banking.     │
                          │   public.      │
                          │   transfers    │
                          └───────┬────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
                    ▼             ▼             ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │ Audit    │ │ Notif.   │ │ External │
            │ Service  │ │ Service  │ │ Systems  │
            └──────────┘ └──────────┘ └──────────┘
```

---

## 🧪 Testing

### Run All Tests
```bash
./mvnw test
```

### Test Specific Service
```bash
./mvnw test -pl validation-service
./mvnw test -pl transfer-service
```

### Test with Coverage
```bash
./mvnw test jacoco:report
```

### Test Results (v2.0)
```
✅ 31 tests passing
✅ 0 failures
✅ 0 errors
✅ 100% build success
```

**Test Coverage by Service:**
- Validation Service: 4 tests
- Notification Service: 11 tests
- Transfer Service: 16 tests (domain + workflow)

---

## 🔍 Monitoring & Observability

### Web Dashboards

| Dashboard | URL | Purpose |
|-----------|-----|---------|
| **Temporal UI** | http://localhost:8088 | Workflow monitoring, retry, debug |
| **Kafka UI** | http://localhost:8090 | Topic visualization, consumer lag |
| **Debezium API** | http://localhost:8083 | CDC connector status |

### Key Endpoints

```bash
# Health checks
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics
curl http://localhost:8082/actuator/info
```

### Search Attributes in Production

To enable search attributes in production:

```bash
# Register search attributes
temporal operator search-attribute create --namespace default \
  --name TransferAmount --type double

temporal operator search-attribute create --namespace default \
  --name SourceAccount --type keyword

temporal operator search-attribute create --namespace default \
  --name DestinationAccount --type keyword

temporal operator search-attribute create --namespace default \
  --name Currency --type keyword

temporal operator search-attribute create --namespace default \
  --name TransferStatus --type keyword

temporal operator search-attribute create --namespace default \
  --name Priority --type int
```

**Note:** In test environments, search attributes are automatically skipped to prevent errors.

---

## 🛠️ Development

### Project Structure

```
temporal-banking-workflow/
├── common/                          # Shared DTOs and models
├── account-service/                 # Account management
├── transfer-service/                # Transfer orchestration
├── validation-service/              # Validation & fraud detection
├── notification-service/            # Notifications via Kafka
├── audit-service/                   # CDC audit trail
├── config/                          # Infrastructure config
└── scripts/                         # Utility scripts
```

### Key Design Decisions

#### 1. **Idempotency**
- All create operations support idempotency keys
- Database-level unique constraints
- Automatic duplicate detection
- Client-provided or auto-generated keys

#### 2. **Error Handling**
- Distinction between temporary and permanent errors
- Retry policies configured per activity
- Circuit breaker pattern for external services
- Comprehensive logging at all levels

#### 3. **Data Consistency**
- Saga pattern for distributed transactions
- Automatic compensation on failure
- Event sourcing via CDC for audit trail
- Eventual consistency between services

#### 4. **Testability**
- Domain services testable without Spring
- Mock ports for unit tests
- TestContainers for integration tests
- Temporal Testing SDK for workflow tests

---

## 📊 Performance Considerations

### Database
- Connection pooling configured
- JSONB for flexible event storage
- Indexed search attributes
- WAL for CDC without performance impact

### Kafka
- Consumer group configuration
- Auto-commit disabled for reliability
- Offset management for exactly-once processing

### Temporal
- Activity timeouts configured
- Retry policies with exponential backoff
- Workflow task timeout: 1 minute
- Workflow run timeout: 4 hours

---

## 🚨 Troubleshooting

### Common Issues

#### Search Attributes Error
**Symptom:** `INVALID_ARGUMENT: search attribute TransferAmount is not defined`

**Solution:** This is expected in test environments. Search attributes are automatically skipped. In production, register them using the commands above.

#### CDC Not Working
```bash
# Check connector status
curl http://localhost:8083/connectors/banking-connector/status

# Reset CDC
make reset-cdc

# Verify PostgreSQL publication
docker exec banking-postgres psql -U postgres -d banking_demo -c \
  "SELECT * FROM pg_publication;"
```

#### Workflow Stuck
```bash
# Check workflow status in Temporal UI
http://localhost:8088

# Cancel workflow
curl -X POST http://localhost:8082/api/transfers/{workflowId}/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Manual cancellation"}'
```

---

## 📚 Additional Resources

- [docs/](docs/) - Project documentation index
- [docs/REFACTORING_COMPLETE_REPORT.md](docs/REFACTORING_COMPLETE_REPORT.md) - Detailed refactoring summary
- [docs/REFACTORING_PROGRESS.md](docs/REFACTORING_PROGRESS.md) - Phase-by-phase progress tracker
- [request.http](./request.http) - Complete API test examples
- [Temporal Documentation](https://docs.temporal.io)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---

## 📝 License

MIT License - See LICENSE file for details

---

## 🎉 Success Metrics

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| **Build Success** | ✅ | ✅ | ✅ Pass |
| **Tests Passing** | 30+ | 31 | ✅ Pass |
| **Hexagonal Architecture** | 100% | 100% | ✅ Pass |
| **Search Attributes** | Implemented | Implemented | ✅ Pass |
| **Timers** | Implemented | Implemented | ✅ Pass |
| **Documentation** | Complete | Complete | ✅ Pass |

---

**Version:** 2.0.0  
**Last Updated:** March 15, 2026  
**Status:** ✅ Production Ready
