# PHASE 3: Temporal Features Implementation Plan

## Epic 1.3: Search Attributes - Indexação Customizada

### Tasks
- [ ] Configure custom search attributes in Temporal
- [ ] Implement upsertSearchAttributes in MoneyTransferWorkflow
- [ ] Create TransferSearchService for advanced queries
- [ ] Create advanced search endpoints
- [ ] Add tests for search functionality

### Implementation Details

#### 1. Custom Search Attributes Configuration
Need to configure in Temporal server:
- `TransferAmount` (Double) - For amount-based queries
- `SourceAccount` (Keyword) - Source account number
- `DestinationAccount` (Keyword) - Destination account number  
- `Currency` (Keyword) - Currency code
- `TransferStatus` (Keyword) - Current status
- `Priority` (Int) - Transfer priority level

#### 2. Workflow Updates
- Add `Workflow.upsertSearchAttributes()` calls
- Update attributes on status changes
- Add calculated attributes (priority, processing time)

#### 3. Search Service
- Create `TransferSearchService` using Temporal Visibility API
- Implement queries by amount range, account, status, date
- Add aggregation methods (count, avg, sum)

#### 4. REST Endpoints
- `GET /api/transfers/search` - Advanced search with filters
- `GET /api/transfers/search/by-account/{account}` - By account
- `GET /api/transfers/search/by-amount-range` - By amount range
- `GET /api/transfers/analytics/summary` - Aggregated metrics

---

## Epic 1.2: Timers - Delays Configuráveis

### Tasks
- [ ] Extend TransferRequest with delay configurations
- [ ] Implement configurable delay in workflow
- [ ] Implement cancelable timer system
- [ ] Create endpoints for delay management
- [ ] Add tests for timer functionality

### Implementation Details

#### 1. TransferRequest Extensions
```java
// Add to TransferRequest DTO
private Duration delayBeforeExecution;  // Delay before starting
private Duration timeoutAfterDelay;     // Timeout for the delay period
private boolean allowCancelDuringDelay; // Can cancel during delay?
```

#### 2. Workflow Timer Implementation
```java
// In MoneyTransferWorkflowImpl
if (request.getDelayBeforeExecution() != null) {
    Workflow.sleep(request.getDelayBeforeExecution());
}

// Cancelable timer
Promise<Void> timer = Workflow.newTimer(delay);
Promise<Void> cancelSignal = Workflow.await(() -> cancelRequested);
Promise.anyOf(timer, cancelSignal).get();
```

#### 3. Delay Management Endpoints
- `POST /api/transfers/{workflowId}/cancel-delay` - Cancel pending delay
- `GET /api/transfers/{workflowId}/delay-status` - Get delay status
- `PUT /api/transfers/{workflowId}/extend-delay` - Extend delay period

---

## Epic 2.1: Child Workflows - Transferências em Lote

### Tasks
- [ ] Create BatchTransferRequest/Response DTOs
- [ ] Create BatchTransferWorkflow interface
- [ ] Implement BatchTransferWorkflowImpl
- [ ] Implement child workflow coordination
- [ ] Create BatchTransferService
- [ ] Create batch endpoints
- [ ] Add tests for batch processing

### Implementation Details

#### 1. Batch DTOs
```java
// BatchTransferRequest
private List<TransferRequest> transfers;
private String batchId;
private boolean parallelExecution;

// BatchTransferResponse  
private String batchId;
int totalTransfers;
int successfulTransfers;
int failedTransfers;
BatchStatus status;
```

#### 2. Child Workflow Pattern
```java
// Parent workflow
List<Promise<TransferResponse>> childPromises = new ArrayList<>();

for (TransferRequest request : batchRequest.getTransfers()) {
    MoneyTransferWorkflow child = Workflow.newChildWorkflowStub(
        MoneyTransferWorkflow.class,
        ChildWorkflowOptions.newBuilder()
            .setWorkflowId("transfer-" + request.getTransferId())
            .build()
    );
    
    Promise<TransferResponse> promise = Async.function(
        child::executeTransfer, request
    );
    childPromises.add(promise);
}

// Wait for all children
Promise.allOf(childPromises).get();
```

#### 3. Batch Endpoints
- `POST /api/transfers/batch` - Create batch transfer
- `GET /api/transfers/batch/{batchId}` - Get batch status
- `GET /api/transfers/batch/{batchId}/results` - Get detailed results
- `POST /api/transfers/batch/{batchId}/cancel` - Cancel entire batch

---

## Implementation Order

### Priority 1: Search Attributes (High Impact, Low Complexity)
1. Configure search attributes in Temporal
2. Update MoneyTransferWorkflow to use search attributes
3. Create TransferSearchService
4. Create REST endpoints
5. Test and document

### Priority 2: Child Workflows (High Impact, Medium Complexity)
1. Create batch DTOs
2. Create child workflow interface and implementation
3. Implement parent workflow for batch coordination
4. Create batch service and endpoints
5. Test parallel and sequential execution

### Priority 3: Timers (Medium Impact, Low Complexity)
1. Extend TransferRequest with delay fields
2. Update workflow to handle delays
3. Implement cancelable timers
4. Create delay management endpoints
5. Test timer functionality

---

## Success Criteria

### Search Attributes
- [ ] Can query transfers by amount range
- [ ] Can query transfers by account number
- [ ] Can query transfers by status
- [ ] Can get aggregated statistics
- [ ] Search results returned in < 1 second

### Child Workflows
- [ ] Can submit batch of 100+ transfers
- [ ] Child workflows execute in parallel (when configured)
- [ ] Partial failures handled correctly
- [ ] Batch status accurately reflects progress
- [ ] Can cancel entire batch

### Timers
- [ ] Can schedule transfer with delay
- [ ] Can cancel transfer during delay period
- [ ] Timer cancellation is immediate
- [ ] Delays are accurate (within 1 second tolerance)
- [ ] Workflow survives worker restarts during delay

---

## Estimated Effort

| Epic | Complexity | Estimated Time |
|------|-----------|----------------|
| Search Attributes | ⭐⭐ | 4-6 hours |
| Child Workflows | ⭐⭐⭐ | 8-10 hours |
| Timers | ⭐⭐ | 4-6 hours |
| **Total** | | **16-22 hours** |

---

## Notes

- All features should include comprehensive tests
- Update documentation after each epic
- Use feature flags to control rollout
- Monitor performance impact
- Document any Temporal server configuration changes required
