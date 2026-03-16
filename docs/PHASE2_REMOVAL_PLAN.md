# Phase 2: Duplicate Code Removal Plan

## Analysis of Duplicate Code in Transfer-Service

### Old Architecture (To Be Removed)
Located in `transfer-service/src/main/java/com/example/temporal/transfer/`

#### 1. service/TransferService.java (OLD)
- **Purpose**: Initiates transfers and manages workflow execution
- **Dependencies**: WorkflowClient, TransferPersistenceService
- **Key Methods**:
  - `initiateTransferAsync(TransferRequest)` - Creates transfer and starts workflow
  - `executeTransferWorkflowAsync()` - Async workflow execution
  - `getTransferById(Long)` - Gets transfer from DB
  - `getTransferStatus(String workflowId)` - Gets status from DB + workflow
  - `getTransfersByAccount(String)` - Lists transfers by account

#### 2. service/TransferControlService.java (OLD)
- **Purpose**: Controls workflow execution (pause/resume/cancel)
- **Dependencies**: WorkflowClient
- **Key Methods**:
  - `pauseTransfer(String workflowId)`
  - `resumeTransfer(String workflowId)`
  - `cancelTransfer(String workflowId, String reason)`
  - `getControlStatus(String workflowId)`
  - `isWorkflowActive(String workflowId)` - Helper method

#### 3. controller/TransferController.java (OLD)
- **Purpose**: REST API controller
- **Dependencies**: TransferService (old), TransferControlService (old), FeatureFlagService
- **Key Endpoints**:
  - POST /api/transfers
  - GET /api/transfers/{id}/status
  - GET /api/transfers/workflow/{workflowId}
  - GET /api/transfers/account/{accountNumber}
  - GET /api/transfers/transfer/{transferId}
  - POST /api/transfers/{workflowId}/pause
  - POST /api/transfers/{workflowId}/resume
  - POST /api/transfers/{workflowId}/cancel
  - GET /api/transfers/{workflowId}/control-status
  - POST /api/transfers/batch/pause
  - POST /api/transfers/batch/cancel

#### 4. service/TransferPersistenceService.java
- **Purpose**: Database operations
- **Status**: Used by OLD architecture only
- **Action**: Can be removed (replaced by TransferPersistencePort)

#### 5. service/FeatureFlagService.java
- **Purpose**: Feature flag management
- **Status**: Still useful, but should be moved to config package
- **Action**: Keep but relocate

### New Architecture (To Keep)
Located in `transfer-service/src/main/java/com/example/temporal/transfer/`

#### 1. domain/service/TransferService.java (NEW)
- **Purpose**: Domain service implementing InitiateTransferUseCase and QueryTransferUseCase
- **Dependencies**: TransferPersistencePort, WorkflowOrchestrationPort (interfaces)
- **Key Methods**:
  - `initiateTransfer(InitiateTransferCommand)` - Creates transfer with idempotency
  - `getTransferById(Long)` - Gets transfer from DB
  - `getTransferByWorkflowId(String)` - Gets transfer by workflow ID
  - `getTransferStatus(String workflowId)` - Gets status from DB + workflow
  - `getTransfersByAccount(String)` - Lists transfers by account

#### 2. domain/service/TransferControlService.java (NEW)
- **Purpose**: Domain service implementing ControlTransferUseCase
- **Dependencies**: TransferPersistencePort, WorkflowOrchestrationPort (interfaces)
- **Key Methods**:
  - `pauseTransfer(String workflowId)` - Pauses workflow
  - `resumeTransfer(String workflowId)` - Resumes workflow
  - `cancelTransfer(String workflowId, String reason)` - Cancels workflow
  - `getControlStatus(String workflowId)` - Gets control status

#### 3. infrastructure/adapter/in/rest/TransferRestController.java (NEW)
- **Purpose**: REST API adapter using domain use cases
- **Dependencies**: InitiateTransferUseCase, QueryTransferUseCase, ControlTransferUseCase, FeatureFlagService
- **Key Endpoints**: Same as old controller

## Migration Strategy

### Step 1: Verify New Architecture Completeness
- [x] New TransferService has all methods from old service
- [x] New TransferControlService has all methods from old service
- [x] New TransferRestController has all endpoints from old controller
- [x] New architecture uses interfaces (ports) instead of concrete implementations

### Step 2: Update Component Scan Configuration
- [ ] Update TransferServiceApplication.java to:
  - Exclude old service package from component scan
  - Include only domain and infrastructure packages

### Step 3: Remove Old Files
- [ ] Delete service/TransferService.java (OLD)
- [ ] Delete service/TransferControlService.java (OLD)
- [ ] Delete controller/TransferController.java (OLD)
- [ ] Delete service/TransferPersistenceService.java
- [ ] Move service/FeatureFlagService.java to config/FeatureFlagService.java

### Step 4: Update Imports and References
- [ ] Update TransferRestController imports (remove old service references)
- [ ] Update any test files that reference old classes
- [ ] Update any other files that reference old classes

### Step 5: Verify Build
- [ ] Run `mvn clean compile` to verify no compilation errors
- [ ] Run tests to verify functionality

### Step 6: Update Documentation
- [ ] Update REFACTORING_PROGRESS.md with completed tasks
- [ ] Document the migration in CHANGELOG.md

## Files to Delete (5 files)
1. `service/TransferService.java` (OLD - duplicate)
2. `service/TransferControlService.java` (OLD - duplicate)
3. `controller/TransferController.java` (OLD - duplicate)
4. `service/TransferPersistenceService.java` (replaced by port/adapter)
5. Any unused files in service/ package

## Files to Move (1 file)
1. `service/FeatureFlagService.java` → `config/FeatureFlagService.java`

## Files to Update (2 files)
1. `TransferServiceApplication.java` - Update component scan
2. `TransferRestController.java` - Already correct, just verify imports

## Risk Mitigation
- **Backup**: Git commit before starting deletion
- **Rollback**: Can revert git commit if issues found
- **Testing**: Run full test suite after each deletion
- **Incremental**: Delete one file at a time, verify build after each

## Success Criteria
- [ ] All old duplicate files removed
- [ ] Build succeeds without errors
- [ ] All tests pass
- [ ] REST API endpoints work correctly
- [ ] No references to old classes remain
