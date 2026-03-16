package com.example.temporal.transfer.domain.port.out;

import com.example.temporal.transfer.domain.model.TransferDomain;

/**
 * Output port (driven port) for workflow orchestration
 * Defines contract for starting and managing workflows without exposing Temporal
 */
public interface WorkflowOrchestrationPort {

    /**
     * Start a transfer workflow
     */
    void startTransferWorkflow(TransferDomain transfer, String workflowId);

    /**
     * Pause a running workflow
     */
    void pauseWorkflow(String workflowId);

    /**
     * Resume a paused workflow
     */
    void resumeWorkflow(String workflowId);

    /**
     * Cancel a workflow
     */
    void cancelWorkflow(String workflowId, String reason);

    /**
     * Get workflow status
     */
    WorkflowStatus getWorkflowStatus(String workflowId);

    /**
     * Check if workflow is running
     */
    boolean isWorkflowRunning(String workflowId);

    @lombok.Value
    @lombok.Builder
    class WorkflowStatus {
        String workflowId;
        String status;
        boolean isPaused;
        boolean isCancelled;
        String pauseReason;
        String cancelReason;
    }
}
