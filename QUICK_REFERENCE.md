# ⚡ Quick Reference Card

> Common commands and shortcuts for daily development

---

## 🚀 Quick Start

```bash
# First time setup
make setup

# Start development environment
make -f Makefile.dev dev-start

# Stop everything
make -f Makefile.dev dev-stop
```

---

## 📦 Build Commands

```bash
# Build all services
./mvnw clean package

# Build specific service
./mvnw clean package -pl account-service

# Build without tests
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run tests for specific service
./mvnw test -pl validation-service
```

---

## ▶️ Service Management

```bash
# Start all services
make -f Makefile.dev dev-start

# Restart specific service
make -f Makefile.dev dev-restart SERVICE=transfer-service

# Stop all services
make -f Makefile.dev dev-stop

# View all logs
make -f Makefile.dev dev-logs-all

# View specific service logs
make -f Makefile.dev dev-logs SERVICE=account-service

# Tail logs in real-time
make -f Makefile.dev dev-tail-logs
```

---

## 🧪 Testing

```bash
# Test CDC flow
make test-cdc

# Test transfer flow
make test-transfer

# Test CDC data retrieval
make test-cdc-data

# Full system diagnosis
make debug-all
```

---

## 🔧 Infrastructure

```bash
# Start all infrastructure
docker-compose up -d

# Stop all infrastructure
docker-compose down

# View infrastructure logs
docker-compose logs -f

# Restart specific component
docker-compose restart postgres
docker-compose restart kafka
docker-compose restart temporal

# Check container status
docker-compose ps
```

---

## 🔍 Monitoring URLs

| Service | URL |
|---------|-----|
| Temporal UI | http://localhost:8088 |
| Kafka UI | http://localhost:8090 |
| Debezium API | http://localhost:8083 |
| Account Service Health | http://localhost:8081/actuator/health |
| Transfer Service Health | http://localhost:8082/actuator/health |
| Audit Service Health | http://localhost:8085/actuator/health |

---

## 📡 Quick API Tests

```bash
# Health check all services
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health

# Create test account
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"accountNumber":"TEST001","ownerName":"Test","balance":1000,"currency":"USD"}'

# Initiate test transfer
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber":"TEST001","destinationAccountNumber":"TEST002","amount":100,"currency":"USD"}'
```

---

## 🐛 Debugging

```bash
# Debug CDC
make debug-cdc

# Debug Temporal
make debug-temporal

# Debug Kafka (live)
make debug-kafka-live

# Check for orphans
make check-orphans

# Reset CDC
make reset-cdc

# Reset Temporal workflows
make reset-temporal
```

---

## 📊 Service Ports

| Service | Port |
|---------|------|
| Account Service | 8081 |
| Transfer Service | 8082 |
| Audit Service | 8085 |
| Notification Service | 8086 |
| Validation Service | 8087 |
| PostgreSQL (Main) | 5432 |
| PostgreSQL (Audit) | 5433 |
| Temporal | 7233 |
| Kafka | 9092 |
| Debezium | 8083 |

---

## 🗂️ Useful File Paths

```
# Documentation
GETTING_STARTED.md          # Main getting started guide
API_EXAMPLES.md             # Complete API examples
request.http                # HTTP request file for testing

# Configuration
.env                        # Environment variables
docker-compose.yml          # Infrastructure definition
application.yml             # Service configuration (in each service)

# Scripts
config/scripts/             # Setup and test scripts
```

---

## 🎯 Common Workflows

### New Developer Setup
```bash
git clone <repo>
cd temporal-banking-workflow
make setup
make -f Makefile.dev dev-start
make debug-all
```

### Daily Development
```bash
make -f Makefile.dev dev-start    # Morning
# ... code ...
make -f Makefile.dev dev-stop     # Evening
```

### Testing Changes
```bash
./mvnw clean package              # Build
make -f Makefile.dev dev-restart SERVICE=account-service  # Restart
make -f Makefile.dev dev-logs SERVICE=account-service     # Check logs
```

### Troubleshooting
```bash
make debug-all                    # Full diagnosis
make -f Makefile.dev dev-logs-all # Check all logs
make reset-cdc                    # Reset if needed
```

---

## 📖 Documentation

- **Getting Started:** [GETTING_STARTED.md](GETTING_STARTED.md)
- **API Examples:** [API_EXAMPLES.md](API_EXAMPLES.md)
- **Architecture:** `/docs/ARCHITECTURE_DIAGRAM.md`
- **Makefile Guide:** `/docs/MAKEFILE_GUIDE.md`

---

**Print this page for quick reference!** 📄
