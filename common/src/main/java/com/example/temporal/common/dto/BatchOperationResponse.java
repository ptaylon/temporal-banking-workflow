package com.example.temporal.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Response for batch transfer operations.
 * Provides structured response for bulk pause/cancel operations.
 */
@Data
@Builder
@Accessors(chain = true)
public class BatchOperationResponse {

    /**
     * Total number of items in the batch
     */
    private int total;

    /**
     * Number of successful operations
     */
    private long successful;

    /**
     * Number of failed operations
     */
    private long failed;

    /**
     * Optional reason for the batch operation (for cancel)
     */
    private String reason;

    /**
     * Individual operation results
     */
    private List<BatchOperationResult> results;

    /**
     * Individual batch operation result
     */
    @Data
    @Builder
    @Accessors(chain = true)
    public static class BatchOperationResult {
        private String workflowId;
        private boolean success;
        private String message;
    }
}
