# 📖 Temporal Banking Workflow - Complete API Examples

> Quick reference for all API endpoints with ready-to-use examples

---

## Table of Contents

- [Account Service](#account-service)
- [Transfer Service](#transfer-service)
- [Audit Service](#audit-service)
- [Health Checks](#health-checks)
- [Complete Workflows](#complete-workflows)

---

## Account Service (Port 8081)

### Create Account

```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC001",
    "ownerName": "John Doe",
    "balance": 1000.00,
    "currency": "USD"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "accountNumber": "ACC001",
    "ownerName": "John Doe",
    "balance": 1000.00,
    "currency": "USD",
    "status": "ACTIVE"
  },
  "message": "Account created successfully"
}
```

### Get Account by Number

```bash
curl http://localhost:8081/api/accounts/ACC001
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "accountNumber": "ACC001",
    "ownerName": "John Doe",
    "balance": 1000.00,
    "currency": "USD",
    "status": "ACTIVE"
  }
}
```

### Update Account

```bash
curl -X PUT http://localhost:8081/api/accounts/ACC001 \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "John Doe Jr.",
    "balance": 1500.00
  }'
```

---

## Transfer Service (Port 8082)

### Basic Transfer

```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "ACC001",
    "destinationAccountNumber": "ACC002",
    "amount": 100.00,
    "currency": "USD"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "transferId": 1,
    "workflowId": "transfer-1",
    "status": "PENDING"
  },
  "message": "Transfer initiated successfully"
}
```

### Transfer with Idempotency

```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-abc123" \
  -d '{
    "sourceAccountNumber": "ACC001",
    "destinationAccountNumber": "ACC002",
    "amount": 100.00,
    "currency": "USD"
  }'
```

### Transfer with Delay

```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "ACC001",
    "destinationAccountNumber": "ACC002",
    "amount": 250.00,
    "currency": "USD",
    "delayInSeconds": 60,
    "allowCancelDuringDelay": true
  }'
```

### Transfer with Timeout

```bash
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "ACC001",
    "destinationAccountNumber": "ACC002",
    "amount": 500.00,
    "currency": "USD",
    "timeoutInSeconds": 300
  }'
```

### Get Transfer by Workflow ID

```bash
curl http://localhost:8082/api/transfers/transfer-1
```

**Response:**
```json
{
  "success": true,
  "data": {
    "transferId": 1,
    "workflowId": "transfer-1",
    "sourceAccount": "ACC001",
    "destinationAccount": "ACC002",
    "amount": 100.00,
    "currency": "USD",
    "status": "COMPLETED",
    "createdAt": "2026-03-19T10:00:00Z",
    "completedAt": "2026-03-19T10:00:05Z"
  }
}
```

### Get Transfer by Transfer ID

```bash
curl http://localhost:8082/api/transfers/1/status
```

### Get Transfers by Account

```bash
curl http://localhost:8082/api/transfers/account/ACC001
```

### Pause Workflow

```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/pause
```

**Response:**
```json
{
  "success": true,
  "message": "Workflow transfer-1 paused successfully"
}
```

### Resume Workflow

```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/resume
```

**Response:**
```json
{
  "success": true,
  "message": "Workflow transfer-1 resumed successfully"
}
```

### Cancel Workflow

```bash
curl -X POST http://localhost:8082/api/transfers/transfer-1/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer request"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Workflow transfer-1 cancelled: Customer request"
}
```

### Get Workflow Control Status

```bash
curl http://localhost:8082/api/transfers/transfer-1/control-status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "workflowId": "transfer-1",
    "isPaused": false,
    "isCancelled": false,
    "cancelReason": null,
    "pauseReason": null
  }
}
```

### Advanced Search

```bash
# Search by status
curl "http://localhost:8082/api/transfers/search?status=COMPLETED"

# Search by amount range
curl "http://localhost:8082/api/transfers/search/by-amount-range?min=100&max=1000"

# Search by account and status
curl "http://localhost:8082/api/transfers/search?account=ACC001&status=COMPLETED"

# Search with pagination
curl "http://localhost:8082/api/transfers/search?page=0&size=20"
```

### Search High-Priority Transfers

```bash
# Priority 4-5 (≥ $50,000)
curl http://localhost:8082/api/transfers/search/high-priority
```

### Get Analytics Summary

```bash
curl http://localhost:8082/api/transfers/search/analytics/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalTransfers": 150,
    "totalAmount": 1250000.00,
    "averageAmount": 8333.33,
    "byStatus": {
      "COMPLETED": 140,
      "PENDING": 5,
      "FAILED": 3,
      "CANCELLED": 2
    },
    "byPriority": {
      "5": 10,
      "4": 25,
      "3": 50,
      "2": 40,
      "1": 20,
      "0": 5
    }
  }
}
```

### Batch Pause

```bash
curl -X POST http://localhost:8082/api/transfers/batch/pause \
  -H "Content-Type: application/json" \
  -d '{
    "workflowIds": ["transfer-1", "transfer-2", "transfer-3"]
  }'
```

### Batch Cancel

```bash
curl -X POST http://localhost:8082/api/transfers/batch/cancel \
  -H "Content-Type: application/json" \
  -d '{
    "workflowIds": ["transfer-1", "transfer-2"],
    "reason": "System maintenance"
  }'
```

---

## Audit Service (Port 8085)

### Get Audit Events by Account

```bash
curl http://localhost:8085/api/audit/accounts/ACC001
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "eventType": "ACCOUNT_CREATED",
      "entityType": "Account",
      "entityId": "ACC001",
      "beforeState": null,
      "afterState": {
        "accountNumber": "ACC001",
        "ownerName": "John Doe",
        "balance": 1000.00
      },
      "timestamp": "2026-03-19T10:00:00Z"
    },
    {
      "id": 2,
      "eventType": "ACCOUNT_UPDATED",
      "entityType": "Account",
      "entityId": "ACC001",
      "beforeState": {
        "balance": 1000.00
      },
      "afterState": {
        "balance": 900.00
      },
      "timestamp": "2026-03-19T10:05:00Z"
    }
  ]
}
```

### Search Audit Events

```bash
# By event type
curl "http://localhost:8085/api/audit/search?eventType=TRANSFER_COMPLETED"

# By entity type
curl "http://localhost:8085/api/audit/search?entityType=Transfer"

# By date range
curl "http://localhost:8085/api/audit/search?startDate=2026-03-19T00:00:00&endDate=2026-03-19T23:59:59"

# Combined search
curl "http://localhost:8085/api/audit/search?entityType=Transfer&eventType=TRANSFER_COMPLETED&limit=50"
```

### Get Recent Events

```bash
curl http://localhost:8085/api/audit/events
```

---

## Health Checks

### Individual Service Health

```bash
# Account Service
curl http://localhost:8081/actuator/health

# Transfer Service
curl http://localhost:8082/actuator/health

# Validation Service
curl http://localhost:8087/actuator/health

# Notification Service
curl http://localhost:8086/actuator/health

# Audit Service
curl http://localhost:8085/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Detailed Health

```bash
curl http://localhost:8081/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

## Complete Workflows

### Scenario 1: Basic Transfer Flow

```bash
# Step 1: Create accounts
echo "=== Creating Source Account ==="
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "SRC001",
    "ownerName": "Alice Johnson",
    "balance": 5000.00,
    "currency": "USD"
  }'

echo -e "\n\n=== Creating Destination Account ==="
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "DST001",
    "ownerName": "Bob Smith",
    "balance": 100.00,
    "currency": "USD"
  }'

# Step 2: Initiate transfer
echo -e "\n\n=== Initiating Transfer ==="
TRANSFER_RESPONSE=$(curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "SRC001",
    "destinationAccountNumber": "DST001",
    "amount": 500.00,
    "currency": "USD"
  }')

echo $TRANSFER_RESPONSE | jq .

# Step 3: Extract workflow ID
WORKFLOW_ID=$(echo $TRANSFER_RESPONSE | jq -r '.data.workflowId')
echo -e "\n\nWorkflow ID: $WORKFLOW_ID"

# Step 4: Check status (wait a few seconds)
sleep 3
echo -e "\n\n=== Checking Transfer Status ==="
curl http://localhost:8082/api/transfers/$WORKFLOW_ID | jq .

# Step 5: View audit trail
echo -e "\n\n=== Audit Trail for Source Account ==="
curl http://localhost:8085/api/audit/accounts/SRC001 | jq .
```

### Scenario 2: High-Value Transfer with Monitoring

```bash
# Create VIP accounts
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "VIP001",
    "ownerName": "Corporate Client",
    "balance": 500000.00,
    "currency": "USD"
  }'

curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "VIP002",
    "ownerName": "Investment Partner",
    "balance": 250000.00,
    "currency": "USD"
  }'

# Initiate high-value transfer (Priority 5)
curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "VIP001",
    "destinationAccountNumber": "VIP002",
    "amount": 150000.00,
    "currency": "USD"
  }'

# Search for high-priority transfers
curl http://localhost:8082/api/transfers/search/high-priority | jq .
```

### Scenario 3: Scheduled Transfer with Cancellation

```bash
# Create accounts
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "SCH001",
    "ownerName": "Scheduled Sender",
    "balance": 2000.00,
    "currency": "USD"
  }'

curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "SCH002",
    "ownerName": "Scheduled Receiver",
    "balance": 500.00,
    "currency": "USD"
  }'

# Schedule transfer for 60 seconds from now
SCHEDULED_RESPONSE=$(curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "SCH001",
    "destinationAccountNumber": "SCH002",
    "amount": 300.00,
    "currency": "USD",
    "delayInSeconds": 60,
    "allowCancelDuringDelay": true
  }')

WORKFLOW_ID=$(echo $SCHEDULED_RESPONSE | jq -r '.data.workflowId')
echo "Scheduled workflow: $WORKFLOW_ID"

# Check status during delay period
echo "=== Status during delay ==="
curl http://localhost:8082/api/transfers/$WORKFLOW_ID | jq .

# Cancel during delay
echo -e "\n=== Cancelling scheduled transfer ==="
curl -X POST http://localhost:8082/api/transfers/$WORKFLOW_ID/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Changed mind"}' | jq .
```

### Scenario 4: Pause and Resume Workflow

```bash
# Initiate transfer
TRANSFER_RESPONSE=$(curl -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "ACC001",
    "destinationAccountNumber": "ACC002",
    "amount": 1000.00,
    "currency": "USD"
  }')

WORKFLOW_ID=$(echo $TRANSFER_RESPONSE | jq -r '.data.workflowId')

# Pause the workflow
echo "=== Pausing workflow ==="
curl -X POST http://localhost:8082/api/transfers/$WORKFLOW_ID/pause | jq .

# Check control status
echo -e "\n=== Control status ==="
curl http://localhost:8082/api/transfers/$WORKFLOW_ID/control-status | jq .

# Resume the workflow
echo -e "\n=== Resuming workflow ==="
curl -X POST http://localhost:8082/api/transfers/$WORKFLOW_ID/resume | jq .
```

---

## HTTP File Examples (request.http)

If you use IntelliJ IDEA or VS Code with REST Client extension, you can use the `request.http` file for easy API testing.

---

## Error Responses

### Validation Error

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Amount must be greater than zero",
    "timestamp": "2026-03-19T10:00:00Z"
  }
}
```

### Not Found Error

```json
{
  "success": false,
  "error": {
    "code": "ENTITY_NOT_FOUND",
    "message": "Account not found: ACC999",
    "timestamp": "2026-03-19T10:00:00Z"
  }
}
```

### Insufficient Funds

```json
{
  "success": false,
  "error": {
    "code": "OPERATION_FAILED",
    "message": "Insufficient funds in account: ACC001",
    "timestamp": "2026-03-19T10:00:00Z"
  }
}
```

---

## Priority Levels

Transfers are automatically assigned priority based on amount:

| Priority | Amount | Description |
|----------|--------|-------------|
| 5 | ≥ $100,000 | VIP |
| 4 | $50,000 - $99,999 | High Value |
| 3 | $10,000 - $49,999 | Medium-High |
| 2 | $1,000 - $9,999 | Standard |
| 1 | $100 - $999 | Small |
| 0 | < $100 | Micro |

---

## Tips

1. **Use idempotency keys** for production transfers to prevent duplicates
2. **Monitor Temporal UI** at http://localhost:8088 for workflow details
3. **Check audit trail** to see all changes via CDC
4. **Use search attributes** to find workflows by criteria
5. **Test with delays** to practice cancellation scenarios

---

**For more details, see [GETTING_STARTED.md](GETTING_STARTED.md)**
