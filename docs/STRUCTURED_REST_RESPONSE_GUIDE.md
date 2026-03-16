# Structured REST API Response Guide

This document describes the move from unstructured `Map` responses to proper DTOs for all REST API error and batch responses.

## Overview

**Before:**
```java
return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", "Control functionality is temporarily disabled"));
```

**After:**
```java
return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.simple("Control functionality is temporarily disabled"));
```

## Why Use Structured DTOs?

### Problems with `Map.of()`:

1. **No Type Safety** - Compiler can't check field names or types
2. **Poor Documentation** - API consumers don't know what fields to expect
3. **Hard to Maintain** - Changing structure requires finding all usages
4. **No Validation** - No guarantee of consistent structure
5. **Poor IDE Support** - No autocomplete for Map keys

### Benefits of Structured DTOs:

1. **Type Safety** - Compiler validates field names and types
2. **Self-Documenting** - Clear structure with JavaDoc
3. **Easy to Maintain** - Change in one place affects all usages
4. **Validation Support** - Can add Bean Validation annotations
5. **Great IDE Support** - Autocomplete and refactoring support
6. **OpenAPI/Swagger** - Generates better API documentation

## New DTOs Created

### 1. `ErrorResponse.java`

Standard error response for all REST API endpoints.

```java
@Data
@Builder
@Accessors(chain = true)
public class ErrorResponse {
    private String message;        // Error message
    private String code;           // Error code for programmatic handling
    private LocalDateTime timestamp; // When error occurred
    private Object details;        // Additional context (optional)
    
    // Factory methods
    public static ErrorResponse simple(String message);
    public static ErrorResponse withCode(String message, String code);
    public static ErrorResponse withDetails(String message, Object details);
}
```

**Example Response:**
```json
{
  "message": "Control functionality is temporarily disabled",
  "code": null,
  "timestamp": "2026-03-15T23:39:40.123",
  "details": null
}
```

**With Details:**
```json
{
  "message": "Workflow not found",
  "code": null,
  "timestamp": "2026-03-15T23:39:40.123",
  "details": {
    "workflowId": "transfer-123"
  }
}
```

### 2. `BatchOperationResponse.java`

Structured response for batch operations (pause/cancel multiple transfers).

```java
@Data
@Builder
@Accessors(chain = true)
public class BatchOperationResponse {
    private int total;                      // Total operations
    private long successful;                // Success count
    private long failed;                    // Failure count
    private String reason;                  // Optional reason (for cancel)
    private List<BatchOperationResult> results; // Individual results
    
    @Data
    @Builder
    @Accessors(chain = true)
    public static class BatchOperationResult {
        private String workflowId;
        private boolean success;
        private String message;
    }
}
```

**Example Response:**
```json
{
  "total": 3,
  "successful": 2,
  "failed": 1,
  "reason": "Batch cancellation",
  "results": [
    {
      "workflowId": "transfer-1",
      "success": true,
      "message": "Transfer cancelled successfully"
    },
    {
      "workflowId": "transfer-2",
      "success": false,
      "message": "Transfer already completed"
    },
    {
      "workflowId": "transfer-3",
      "success": true,
      "message": "Transfer cancelled successfully"
    }
  ]
}
```

## Refactored Endpoints

### TransferRestController

#### Error Responses

**Before:**
```java
@PostMapping("/{workflowId}/pause")
public ResponseEntity<?> pauseTransfer(@PathVariable String workflowId) {
    if (!featureFlagService.isControlEnabled()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Control functionality is temporarily disabled"));
    }
    // ...
}
```

**After:**
```java
private static final String CONTROL_DISABLED_ERROR = "Control functionality is temporarily disabled";

@PostMapping("/{workflowId}/pause")
public ResponseEntity<?> pauseTransfer(@PathVariable final String workflowId) {
    if (!featureFlagService.isControlEnabled()) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.simple(CONTROL_DISABLED_ERROR));
    }
    // ...
}
```

#### Batch Operations

**Before:**
```java
@PostMapping("/batch/cancel")
public ResponseEntity<?> cancelMultipleTransfers(@RequestBody Map<String, Object> request) {
    List<String> workflowIds = (List<String>) request.get("workflowIds");
    String reason = (String) request.getOrDefault("reason", "Batch cancellation");
    
    // ... processing with Map.of()
    
    return ResponseEntity.ok(Map.of(
        "total", workflowIds.size(),
        "successful", successCount,
        "failed", workflowIds.size() - successCount,
        "reason", reason,
        "results", results
    ));
}
```

**After:**
```java
@PostMapping("/batch/cancel")
public ResponseEntity<BatchOperationResponse> cancelMultipleTransfers(
        @RequestBody final BatchCancelRequest request) {
    
    final List<String> workflowIds = request.getWorkflowIds();
    final String reason = request.getReason() != null 
            ? request.getReason() 
            : "Batch cancellation";
    
    // ... processing with structured DTOs
    
    final BatchOperationResponse response = BatchOperationResponse.builder()
            .total(workflowIds.size())
            .successful(successCount)
            .failed(workflowIds.size() - successCount)
            .reason(reason)
            .results(results)
            .build();
    
    return ResponseEntity.ok(response);
}
```

## API Documentation Benefits

### OpenAPI/Swagger Generation

With structured DTOs, Swagger UI now shows:

**Before (with Map):**
```yaml
responses:
  503:
    description: Service Unavailable
    content:
      application/json:
        schema:
          type: object  # No structure defined
```

**After (with DTOs):**
```yaml
responses:
  503:
    description: Service Unavailable
    content:
      application/json:
        schema:
          $ref: '#/components/schemas/ErrorResponse'
          
components:
  schemas:
    ErrorResponse:
      type: object
      properties:
        message:
          type: string
          description: Error message
        code:
          type: string
          description: Error code
        timestamp:
          type: string
          format: date-time
        details:
          type: object
```

## Usage Guidelines

### When to Use ErrorResponse

1. **Service Unavailable** - Feature disabled, maintenance
2. **Not Found** - Resource doesn't exist
3. **Bad Request** - Invalid input
4. **Internal Server Error** - Unexpected errors
5. **Unauthorized/Forbidden** - Authentication/authorization failures

### When to Use BatchOperationResponse

1. **Batch Pause** - Multiple transfers pause
2. **Batch Cancel** - Multiple transfers cancellation
3. **Batch Resume** - Multiple transfers resume (if implemented)
4. **Any Bulk Operation** - Consistent structure for bulk operations

### Factory Methods

Use the appropriate factory method for clarity:

```java
// Simple error with just message
ErrorResponse.simple("Resource not found");

// Error with code for programmatic handling
ErrorResponse.withCode("Invalid amount", "INVALID_AMOUNT");

// Error with additional context
ErrorResponse.withDetails(
    "Workflow not found",
    Map.of("workflowId", workflowId, "attemptedAt", LocalDateTime.now())
);
```

## Migration Guide

### For API Consumers

**Before:**
```javascript
fetch('/api/transfers/batch/cancel', {
  method: 'POST',
  body: JSON.stringify({
    workflowIds: ['transfer-1', 'transfer-2']
  })
})
.then(response => response.json())
.then(data => {
  // Access via bracket notation
  console.log(data['total']);
  console.log(data['successful']);
});
```

**After:**
```javascript
fetch('/api/transfers/batch/cancel', {
  method: 'POST',
  body: JSON.stringify({
    workflowIds: ['transfer-1', 'transfer-2']
  })
})
.then(response => response.json())
.then(data => {
  // Access via dot notation (clearer)
  console.log(data.total);
  console.log(data.successful);
  console.log(data.results[0].workflowId);
});
```

### For Developers

When adding new endpoints:

1. **Never use `Map.of()`** for responses
2. **Create proper DTOs** for complex structures
3. **Use `ErrorResponse`** for error responses
4. **Add JavaDoc** to all DTO fields
5. **Consider OpenAPI** documentation impact

## Testing Benefits

### Before (with Map):
```java
@Test
void testErrorResponse() {
    ResponseEntity<?> response = controller.pauseTransfer("test-id");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    
    assertEquals("Error message", body.get("error"));
    // Type casting required
    // No compile-time checking
}
```

### After (with DTOs):
```java
@Test
void testErrorResponse() {
    ResponseEntity<ErrorResponse> response = controller.pauseTransfer("test-id");
    
    ErrorResponse body = response.getBody();
    
    assertEquals("Error message", body.getMessage());
    assertNotNull(body.getTimestamp());
    // Type-safe
    // Compile-time checking
}
```

## Best Practices

### 1. Use Constants for Error Messages
```java
private static final String CONTROL_DISABLED_ERROR = 
    "Control functionality is temporarily disabled";

body(ErrorResponse.simple(CONTROL_DISABLED_ERROR));
```

### 2. Add Context with Details
```java
body(ErrorResponse.withDetails(
    "Workflow not found",
    Map.of("workflowId", workflowId)
));
```

### 3. Use Builder for Complex Responses
```java
BatchOperationResponse.builder()
    .total(workflowIds.size())
    .successful(successCount)
    .failed(failed)
    .results(results)
    .build()
```

### 4. Keep DTOs Immutable
```java
@Data
@Builder
@Accessors(chain = true)  // Enables fluent API
public class ErrorResponse {
    // Fields are set via builder
}
```

## Summary

| Aspect | Before (Map) | After (DTOs) |
|--------|-------------|--------------|
| Type Safety | ❌ None | ✅ Full |
| IDE Support | ❌ Poor | ✅ Excellent |
| Documentation | ❌ Manual | ✅ Auto-generated |
| Maintainability | ❌ Hard | ✅ Easy |
| Validation | ❌ None | ✅ Bean Validation |
| API Clarity | ❌ Unclear | ✅ Self-documenting |

---

**Version:** 1.0  
**Date:** March 15, 2026  
**Status:** ✅ Implemented
