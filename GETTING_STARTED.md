# 🏦 Temporal Banking Workflow - Getting Started Guide

> **Version:** 2.0.0 | **Status:** Production Ready ✅ | **Tests:** 31 passing

A complete banking system demonstration built with **Temporal.io** for workflow orchestration, implementing **microservices architecture** with **distributed saga patterns**, **Change Data Capture (CDC)**, and **real-time comprehensive auditing**.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Running the Project](#running-the-project)
- [Testing](#testing)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Monitoring & Dashboards](#monitoring--dashboards)
- [Troubleshooting](#troubleshooting)
- [Configuration](#configuration)

---

## 🎯 Overview

### What This Project Demonstrates

This is a **complete banking workflow system** showcasing:

| Feature | Description |
|---------|-------------|
| **Workflow Orchestration** | Temporal.io for reliable, resilient workflow execution |
| **Saga Pattern** | Distributed transactions with automatic compensation |
| **Change Data Capture** | Real-time audit trail via Debezium + Kafka |
| **Idempotency** | Built-in duplicate prevention across all services |
| **Hexagonal Architecture** | Clean separation of concerns, high testability |
| **Search Attributes** | Query workflows by amount, account, status, priority |
| **Interactive Workflows** | Pause, resume, cancel running transfers |

### System at a Glance

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         BANKING WORKFLOW SYSTEM                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                         │
│  │ Account  │────▶│ Transfer │────▶│Validation│                         │
│  │  :8081   │     │  :8082   │     │  :8087   │                         │
│  └──────────┘     └──────────┘     └──────────┘                         │
│        │                │                                  │            │
│        │                ▼                                  │            │
│        │         ┌──────────┐                              │            │
│        │         │ Temporal │◀─────────────────────────────┘            │
│        │         │   UI     │                                             │
│        │         │  :8088   │                                             │
│        │         └──────────┘                                             │
│        │                │                                                 │
│        ▼                ▼                                                 │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                         │
│  │  Kafka   │◀────│ Debezium │◀────│PostgreSQL│                         │
│  │  :9092   │     │  :8083   │     │  :5432   │                         │
│  └──────────┘     └──────────┘     └──────────┘                         │
│        │                                                                 │
│        ▼                ▼                                                │
│  ┌──────────┐     ┌──────────┐                                          │
│  │Notification│    │  Audit   │                                          │
│  │  :8086   │    │  :8085   │                                          │
│  └──────────┘     └──────────┘                                          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Architecture

### Microservices

| Service | Port | Responsibility |
|---------|------|----------------|
| **Account Service** | 8081 | Account management, balance operations |
| **Transfer Service** | 8082 | Transfer orchestration, Temporal workflows |
| **Validation Service** | 8087 | Transfer validation, fraud detection |
| **Notification Service** | 8086 | Status notifications via Kafka |
| **Audit Service** | 8085 | CDC audit trail, event tracking |

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

### Technology Stack

| Category | Technologies |
|----------|--------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.3, Spring Cloud 2023.0.0 |
| **Workflow** | Temporal.io 1.24.1 |
| **Database** | PostgreSQL 15 |
| **Messaging** | Apache Kafka 7.4.0, Debezium 2.5.0 |
| **Build** | Maven 3.8+ |
| **Testing** | TestContainers 1.19.6, JUnit 5 |
| **Container** | Docker, Docker Compose |

---

## 📦 Prerequisites

Ensure you have the following installed:

| Tool | Version | Verify Command |
|------|---------|----------------|
| **Java** | 21+ | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **Docker** | 20.10+ | `docker --version` |
| **Docker Compose** | v2+ | `docker compose version` |
| **Make** | Any | `make --version` (optional) |

### Install Prerequisites

#### macOS
```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 21
brew install openjdk@21

# Install Maven
brew install maven

# Install Docker Desktop
brew install --cask docker
```

#### Linux (Ubuntu/Debian)
```bash
# Install Java 21
sudo apt update
sudo apt install openjdk-21-jdk

# Install Maven
sudo apt install maven

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

#### Windows
```powershell
# Using Chocolatey
choco install openjdk21 maven docker-desktop
```

---

## 🚀 Quick Start

### One-Command Setup (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd temporal-banking-workflow

# Complete setup (first time only)
make setup

# Start development environment
make -f Makefile.dev dev-start

# Verify everything works
make debug-all
```

### Manual Quick Start

```bash
# 1. Start all infrastructure (PostgreSQL, Kafka, Temporal, etc.)
docker-compose up -d

# 2. Wait for services to be ready (about 60 seconds)
sleep 60

# 3. Configure CDC connector
make setup-cdc

# 4. Build all services
./mvnw clean package -DskipTests

# 5. Start all services (in background)
make -f Makefile.dev dev-start
```

---

## 📥 Installation

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd temporal-banking-workflow
```

### Step 2: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Review and adjust if needed (defaults work for local development)
cat .env
```

### Step 3: Start Infrastructure

```bash
# Start all containers
docker-compose up -d

# Watch startup logs (optional)
docker-compose logs -f
```

**Wait for all services to be healthy:**
```bash
# Check container health
docker-compose ps

# All should show "healthy" or "Up"
```

### Step 4: Configure Debezium CDC

```bash
# Run CDC setup script
make setup-cdc

# Or manually:
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @config/scripts/debezium-connector-config.json
```

### Step 5: Build Services

```bash
# Build all modules
./mvnw clean package -DskipTests

# Or with tests (recommended for first build)
./mvnw clean package
```

### Step 6: Verify Setup

```bash
# Run health checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health

# All should return: {"status":"UP"}
```

---

## ▶️ Running the Project

### Using Makefile (Recommended)

```bash
# Start all services in background
make -f Makefile.dev dev-start

# View logs
make -f Makefile.dev dev-logs-all

# Restart a specific service
make -f Makefile.dev dev-restart SERVICE=account-service

# Stop all services
make -f Makefile.dev dev-stop
```

### Manual Service Startup

Start each service in a **separate terminal**:

```bash
# Terminal 1 - Account Service (Port 8081)
java -jar account-service/target/account-service-1.0-SNAPSHOT.jar

# Terminal 2 - Transfer Service (Port 8082)
java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar

# Terminal 3 - Validation Service (Port 8087)
java -jar validation-service/target/validation-service-1.0-SNAPSHOT.jar

# Terminal 4 - Notification Service (Port 8086)
java -jar notification-service/target/notification-service-1.0-SNAPSHOT.jar

# Terminal 5 - Audit Service (Port 8085)
java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar
```

### Development Mode with Hot Reload

```bash
# Start with Spring DevTools (auto-restart on changes)
java -jar -Dspring.devtools.restart.enabled=true \
  account-service/target/account-service-1.0-SNAPSHOT.jar
```

---

## 🧪 Testing

### Run All Tests

```bash
# Run all tests across all modules
./mvnw test

# Run with code coverage report
./mvnw test jacoco:report
```

### Test Specific Service

```bash
# Validation Service tests
./mvnw test -pl validation-service

# Transfer Service tests
./mvnw test -pl transfer-service

# Account Service tests
./mvnw test -pl account-service
```

### Integration Tests

```bash
# Test CDC flow
make test-cdc
# or
./config/scripts/test-audit-cdc.sh

# Test complete transfer flow
make test-transfer
# or
./config/scripts/test-transfer.sh

# Test CDC data retrieval
make test-cdc-data
```

### Test Results (v2.0)

```
✅ 31 tests passing
✅ 0 failures
✅ 0 errors
✅ 100% build success
```

**Coverage by Service:**
- Validation Service: 4 tests
- Notification Service: 11 tests
- Transfer Service: 16 tests

---

## 📡 API Reference

### Account Service (Port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/accounts` | Create account |
| `GET` | `/api/accounts/{accountNumber}` | Get account details |
| `PUT` | `/api/accounts/{accountNumber}` | Update account |

#### Create Account
```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "123456",
    "ownerName": "John Silva",
    "balance": 1000.00,
    "currency": "BRL"
  }'
```

#### Get Account
```bash
curl http://localhost:8081/api/accounts/123456
```

---

### Transfer Service (Port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transfers` | Initiate transfer |
| `GET` | `/api/transfers/{workflowId}` | Get transfer status |
| `GET` | `/api/transfers/{transferId}/status` | Get by transfer ID |
| `GET` | `/api/transfers/account/{account}` | Get by account |
| `POST` | `/api/transfers/{workflowId}/pause` | Pause workflow |
| `POST` | `/api/transfers/{workflowId}/resume` | Resume workflow |
| `POST` | `/api/transfers/{workflowId}/cancel` | Cancel workflow |
| `GET` | `/api/transfers/search` | Advanced search |
| `GET` | `/api/transfers/search/high-priority` | High priority transfers |

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

#### Transfer with Delay
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

#### Idempotent Transfer
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

#### Pause Workflow
```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/pause
```

#### Resume Workflow
```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/resume
```

#### Cancel Workflow
```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Customer request"}'
```

---

### Audit Service (Port 8085)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/audit/accounts/{account}` | Get audit by account |
| `GET` | `/api/audit/search` | Search audit events |
| `GET` | `/api/audit/events` | Get all recent events |

#### Query Audit Trail
```bash
curl http://localhost:8085/api/audit/accounts/123456
```

---

### Health Checks

All services expose Spring Boot Actuator:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health
```

---

## 💡 Examples

### Complete Transfer Flow

```bash
# 1. Create source account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "SRC001",
    "ownerName": "Alice Johnson",
    "balance": 5000.00,
    "currency": "USD"
  }'

# 2. Create destination account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "DST001",
    "ownerName": "Bob Smith",
    "balance": 100.00,
    "currency": "USD"
  }'

# 3. Initiate transfer
TRANSFER_RESPONSE=$(curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "SRC001",
    "destinationAccountNumber": "DST001",
    "amount": 500.00,
    "currency": "USD"
  }')

# Extract workflow ID
WORKFLOW_ID=$(echo $TRANSFER_RESPONSE | jq -r '.workflowId')
echo "Workflow ID: $WORKFLOW_ID"

# 4. Check transfer status
curl http://localhost:8082/api/transfers/$WORKFLOW_ID

# 5. View audit trail
curl http://localhost:8085/api/audit/accounts/SRC001
```

### High-Value Transfer (Priority 5)

```bash
# Transfer ≥ $100,000 gets Priority 5 (VIP)
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "SRC001",
    "destinationAccountNumber": "DST001",
    "amount": 150000.00,
    "currency": "USD"
  }'

# Search for high-priority transfers
curl http://localhost:8082/api/transfers/search/high-priority
```

### Scheduled Transfer

```bash
# Schedule transfer for 60 seconds from now
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "SRC001",
    "destinationAccountNumber": "DST001",
    "amount": 250.00,
    "currency": "USD",
    "delayInSeconds": 60,
    "allowCancelDuringDelay": true
  }'

# Cancel during delay period
curl -X POST http://localhost:8082/api/transfers/transfer-5/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Changed mind"}'
```

### Batch Operations

```bash
# Pause multiple workflows
curl -X POST http://localhost:8082/api/transfers/batch/pause \
  -H "Content-Type: application/json" \
  -d '{
    "workflowIds": ["transfer-1", "transfer-2", "transfer-3"]
  }'

# Cancel multiple workflows
curl -X POST http://localhost:8082/api/transfers/batch/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "workflowIds": ["transfer-1", "transfer-2"],
    "reason": "System maintenance"
  }'
```

---

## 📊 Monitoring & Dashboards

### Web Dashboards

| Dashboard | URL | Purpose |
|-----------|-----|---------|
| **Temporal UI** | http://localhost:8088 | Workflow monitoring, retry, debug |
| **Kafka UI** | http://localhost:8090 | Topic visualization, consumer lag |
| **Debezium API** | http://localhost:8083 | CDC connector status |

### Temporal UI Features

- **Workflow History** - Complete execution timeline
- **Search & Filter** - By workflow ID, type, status
- **Retry & Debug** - Manually retry failed activities
- **Real-time Status** - Live workflow state updates

### Kafka UI Features

- **Topic Browser** - View messages in real-time
- **Consumer Groups** - Monitor consumer lag
- **Message Inspection** - View payload and headers

### Monitoring Commands

```bash
# View all service logs
make -f Makefile.dev dev-logs-all

# Tail specific service logs
make -f Makefile.dev dev-logs SERVICE=transfer-service

# Check system health
make -f Makefile.dev dev-health-check

# Debug all components
make debug-all
```

---

## 🔧 Troubleshooting

### Common Issues

#### Services Won't Start

```bash
# Check if ports are in use
lsof -i :8081
lsof -i :8082
lsof -i :5432

# Kill conflicting processes
kill -9 <PID>

# Restart services
make -f Makefile.dev dev-restart SERVICE=account-service
```

#### CDC Not Working

```bash
# Check Debezium connector status
curl http://localhost:8083/connectors

# Reset CDC
make reset-cdc

# Reconfigure connector
make setup-cdc
```

#### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

#### Temporal Workflow Stuck

```bash
# View workflow in Temporal UI
# http://localhost:8088/namespaces/default/workflows

# Cancel stuck workflow
curl -X POST http://localhost:8082/api/transfers/transfer-1/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Manual cancellation"}'

# Reset Temporal (last resort)
make reset-temporal
```

### Debugging Tools

```bash
# Full system diagnosis
make debug-all

# CDC debug
make debug-cdc

# Temporal debug
make debug-temporal

# Kafka live monitoring
make debug-kafka-live

# Check for orphaned resources
make check-orphans
```

### Logs Location

```bash
# Service logs (when running via Makefile)
docker logs -f temporal-banking-workflow-account-service-1
docker logs -f temporal-banking-workflow-transfer-service-1

# Infrastructure logs
docker-compose logs -f postgres
docker-compose logs -f kafka
docker-compose logs -f temporal
```

---

## ⚙️ Configuration

### Environment Variables (.env)

```bash
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=banking_demo
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Audit Database Configuration
AUDIT_POSTGRES_HOST=localhost
AUDIT_POSTGRES_PORT=5433
AUDIT_POSTGRES_DB=audit_db
AUDIT_POSTGRES_USER=postgres
AUDIT_POSTGRES_PASSWORD=postgres

# Temporal Configuration
TEMPORAL_HOST=localhost
TEMPORAL_PORT=7233

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Debezium Configuration
DEBEZIUM_CONNECT_URL=http://localhost:8083

# Service URLs
ACCOUNT_SERVICE_URL=http://localhost:8081
TRANSFER_SERVICE_URL=http://localhost:8082
VALIDATION_SERVICE_URL=http://localhost:8087
NOTIFICATION_SERVICE_URL=http://localhost:8086
AUDIT_SERVICE_URL=http://localhost:8085

# Logging
LOG_LEVEL=INFO
ROOT_LOG_LEVEL=WARN

# Development/Production
SPRING_PROFILES_ACTIVE=development
```

### Application Configuration (application.yml)

Each service has its own `application.yml` with service-specific settings:

```yaml
server:
  port: 8081  # Service-specific port

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/banking_demo
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}

temporal:
  host: ${TEMPORAL_HOST:localhost}
  port: ${TEMPORAL_PORT:7233}

kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### Debezium Configuration

CDC connector configuration in `config/scripts/debezium-connector-config.json`:

```json
{
  "name": "banking-postgres-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "banking_demo",
    "database.server.name": "banking",
    "plugin.name": "pgoutput",
    "table.include.list": "public.accounts,public.transfers"
  }
}
```

---

## 📚 Additional Resources

### Documentation Files

Located in `/docs`:

- `ARCHITECTURE_DIAGRAM.md` - System architecture overview
- `HEXAGONAL_ARCHITECTURE.md` - Hexagonal architecture details
- `MAKEFILE_GUIDE.md` - Makefile usage
- `TRANSACTION_MANAGEMENT_GUIDE.md` - Transaction management

### External Resources

- [Temporal Documentation](https://docs.temporal.io)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Debezium Documentation](https://debezium.io/documentation)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation)

---

## 🎉 Next Steps

1. **Explore the Temporal UI** - http://localhost:8088
2. **Run a test transfer** - See [Examples](#examples)
3. **Monitor the workflow** - Watch it execute in real-time
4. **Check the audit trail** - See CDC in action
5. **Read the architecture docs** - Understand the design decisions

---

**Happy Banking! 🏦**

For questions or issues, refer to the troubleshooting section or check the detailed documentation in `/docs`.
