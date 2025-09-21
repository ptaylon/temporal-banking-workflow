# Banking Demo with Temporal.io

This project demonstrates the use of Temporal.io for orchestrating a banking transfer system with multiple microservices.

## Architecture

The system consists of the following microservices:

1. **Account Service** (Port: 8081)
   - Manages bank accounts
   - Handles account balance operations
   - Integrates with Debezium for CDC

2. **Transfer Service** (Port: 8082)
   - Orchestrates money transfers using Temporal workflows
   - Implements saga pattern for distributed transactions
   - Handles compensation in case of failures

3. **Validation Service** (Port: 8087)
   - Validates transfer requests
   - Implements fraud detection rules
   - Enforces transfer limits

4. **Notification Service** (Port: 8086)
   - Handles transfer status notifications
   - Consumes Kafka events
   - Sends email notifications

5. **Audit Service** (Port: 8085)
   - Tracks all operations using CDC
   - Maintains audit trail of all transactions
   - Provides audit history API

## Infrastructure Components

- **PostgreSQL**
  - Main database (Port: 5432)
  - Audit database (Port: 5433)

- **Temporal.io**
  - Server (Port: 7233)
  - Web UI (Port: 8088)

- **Kafka**
  - Broker (Port: 9092)
  - UI (Port: 8090)

- **Debezium**
  - Connect REST API (Port: 8083)

## Setup

1. Start the infrastructure:
   ```bash
   docker-compose up -d
   ```

2. Make the connector registration script executable:
   ```bash
   chmod +x scripts/register-connector.sh
   ```

3. Register the Debezium connector:
   ```bash
   ./scripts/register-connector.sh
   ```

4. Build the services:
   ```bash
   ./mvnw clean package
   ```

5. Start each service:
   ```bash
   java -jar account-service/target/account-service-1.0-SNAPSHOT.jar
   java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar
   java -jar validation-service/target/validation-service-1.0-SNAPSHOT.jar
   java -jar notification-service/target/notification-service-1.0-SNAPSHOT.jar
   java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar
   ```

## Monitoring

- Temporal Web UI: http://localhost:8088
- Kafka UI: http://localhost:8090
- Service health endpoints: http://localhost:{service-port}/actuator/health

## Testing

The project includes unit tests and workflow tests demonstrating:
- Successful transfers
- Validation failures
- Compensation scenarios

Run tests with:
```bash
./mvnw test
```

## Example API Usage

1. Create accounts:
```bash
curl -X POST http://localhost:8081/api/accounts -H "Content-Type: application/json" -d '{
  "accountNumber": "123",
  "ownerName": "John Doe",
  "balance": 1000.00,
  "currency": "USD"
}'
```

```bash
curl -X POST http://localhost:8081/api/accounts -H "Content-Type: application/json" -d '{
  "accountNumber": "123",
  "ownerName": "Carl",
  "balance": 1000.00,
  "currency": "USD"
}'
```

2. Initiate transfer:
```bash
curl -X POST http://localhost:8082/api/transfers -H "Content-Type: application/json" -d '{
  "sourceAccountNumber": "123",
  "destinationAccountNumber": "456",
  "amount": 2.00,
  "currency": "USD"
}'
```

3. Check transfer status:
```bash
curl http://localhost:8082/api/transfers/{workflowId}
```

4. View audit trail:
```bash
curl http://localhost:8085/api/audit/accounts/123
```

## Key Features

- **Temporal Workflows**
  - Automatic retries
  - Compensation handling
  - State persistence
  - Activity timeouts
  - Custom retry policies

- **Data Consistency**
  - CDC with Debezium
  - Saga pattern
  - Transaction compensation
  - Audit trails

- **Monitoring**
  - Health checks
  - Metrics
  - Workflow visualization
  - Kafka monitoring

## Error Handling

The system handles various failure scenarios:
- Validation failures
- Insufficient funds
- Network issues
- Service unavailability
- Concurrent transfers

Each failure type has specific retry policies and compensation logic.