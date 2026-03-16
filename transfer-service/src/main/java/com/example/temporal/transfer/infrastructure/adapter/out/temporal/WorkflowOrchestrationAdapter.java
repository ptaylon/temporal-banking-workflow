package com.example.temporal.transfer.infrastructure.adapter.out.temporal;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.out.WorkflowOrchestrationPort;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Adapter for Temporal workflow orchestration
 * Implements domain port using Temporal SDK
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowOrchestrationAdapter implements WorkflowOrchestrationPort {

    private final WorkflowClient workflowClient;

    @Override
    @Async("transferExecutor")
    public void startTransferWorkflow(TransferDomain transfer, String workflowId) {
        log.info("Starting workflow for transfer ID: {} with workflowId: {}", transfer.getId(), workflowId);

        CompletableFuture.runAsync(() -> {
            try {
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue(MoneyTransferWorkflow.QUEUE_NAME)
                        .setWorkflowId(workflowId)
                        .setWorkflowTaskTimeout(java.time.Duration.ofMinutes(1))
                        .setWorkflowRunTimeout(java.time.Duration.ofHours(4))
                        .build();

                MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                        MoneyTransferWorkflow.class, options);

                // Convert domain to DTO for workflow
                TransferRequest request = toTransferRequest(transfer);

                // Execute workflow (blocking call)
                workflow.executeTransfer(request);

                log.info("Workflow completed for transfer ID: {}", transfer.getId());

            } catch (io.temporal.failure.TerminatedFailure e) {
                log.warn("Workflow terminated externally for transfer ID: {} - Reason: {}",
                        transfer.getId(), e.getMessage());
            } catch (io.temporal.failure.CanceledFailure e) {
                log.info("Workflow cancelled for transfer ID: {} - Reason: {}",
                        transfer.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error executing workflow for transfer ID: {} - {}",
                        transfer.getId(), e.getMessage(), e);
            }
        });
    }

    @Override
    public void pauseWorkflow(String workflowId) {
        log.info("Pausing workflow: {}", workflowId);

        try {
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.pauseTransfer();
            log.info("Workflow paused successfully: {}", workflowId);

        } catch (Exception e) {
            log.error("Error pausing workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to pause workflow: " + e.getMessage(), e);
        }
    }

    @Override
    public void resumeWorkflow(String workflowId) {
        log.info("Resuming workflow: {}", workflowId);

        try {
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.resumeTransfer();
            log.info("Workflow resumed successfully: {}", workflowId);

        } catch (Exception e) {
            log.error("Error resuming workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to resume workflow: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelWorkflow(String workflowId, String reason) {
        log.info("Cancelling workflow: {} with reason: {}", workflowId, reason);

        try {
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.cancelTransfer(reason);
            log.info("Workflow cancelled successfully: {}", workflowId);

        } catch (Exception e) {
            log.error("Error cancelling workflow {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to cancel workflow: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkflowStatus getWorkflowStatus(String workflowId) {
        log.debug("Getting workflow status: {}", workflowId);

        try {
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            TransferControlStatus controlStatus = workflow.getControlStatus();

            return WorkflowStatus.builder()
                    .workflowId(workflowId)
                    .status(controlStatus.getWorkflowStatus())
                    .isPaused(controlStatus.isPaused())
                    .isCancelled(controlStatus.isCancelled())
                    .pauseReason(controlStatus.getPauseReason())
                    .cancelReason(controlStatus.getCancelReason())
                    .build();

        } catch (Exception e) {
            log.error("Error getting workflow status {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to get workflow status: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isWorkflowRunning(String workflowId) {
        log.debug("Checking if workflow is running: {}", workflowId);

        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);

            // Try to describe the workflow - if it throws, workflow doesn't exist or is not running
            workflowStub.getExecution();
            return true;

        } catch (Exception e) {
            log.debug("Workflow {} is not running: {}", workflowId, e.getMessage());
            return false;
        }
    }

    /**
     * Convert domain model to DTO for workflow
     */
    private TransferRequest toTransferRequest(TransferDomain transfer) {
        TransferRequest request = new TransferRequest();
        request.setTransferId(transfer.getId());
        request.setSourceAccountNumber(transfer.getSourceAccountNumber());
        request.setDestinationAccountNumber(transfer.getDestinationAccountNumber());
        request.setAmount(transfer.getAmount());
        request.setCurrency(transfer.getCurrency());
        return request;
    }
}
