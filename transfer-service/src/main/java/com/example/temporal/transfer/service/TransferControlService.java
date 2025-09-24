package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferControlResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferControlService {

    private final WorkflowClient workflowClient;

    private boolean isWorkflowActive(String workflowId) {
        try {
            var workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            var execution = workflowStub.getExecution();
            var result = workflowClient.getWorkflowServiceStubs()
                .blockingStub()
                .describeWorkflowExecution(
                    io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest.newBuilder()
                        .setNamespace(workflowClient.getOptions().getNamespace())
                        .setExecution(execution)
                        .build()
                );
            
            var status = result.getWorkflowExecutionInfo().getStatus();
            return status == io.temporal.api.enums.v1.WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
            
        } catch (Exception e) {
            log.debug("Error checking workflow status for {}: {}", workflowId, e.getMessage());
            return false;
        }
    }

    public TransferControlResponse pauseTransfer(String workflowId) {
        try {
            if (!isWorkflowActive(workflowId)) {
                log.info("Workflow is not active. WorkflowId: {}", workflowId);
                return TransferControlResponse.error(workflowId, "Workflow is not active");
            }

            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.pauseTransfer();

            TransferControlStatus status = workflow.getControlStatus();

            log.info("Transfer paused successfully. WorkflowId: {}", workflowId);
            return TransferControlResponse.success(workflowId, status, "Transfer paused successfully");

        } catch (Exception e) {
            log.error("Error pausing transfer for workflowId {}: {}", workflowId, e.getMessage());
            return TransferControlResponse.error(workflowId, "Failed to pause transfer: " + e.getMessage());
        }
    }

    public TransferControlResponse resumeTransfer(String workflowId) {
        try {
            if (!isWorkflowActive(workflowId)) {
                log.warn("Cannot resume - workflow is not active. WorkflowId: {}", workflowId);
                return TransferControlResponse.error(workflowId, "Workflow is not active or has been terminated");
            }

            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.resumeTransfer();

            TransferControlStatus status = workflow.getControlStatus();

            log.info("Transfer resumed successfully. WorkflowId: {}", workflowId);
            return TransferControlResponse.success(workflowId, status, "Transfer resumed successfully");

        } catch (Exception e) {
            log.error("Error resuming transfer for workflowId {}: {}", workflowId, e.getMessage());
            return TransferControlResponse.error(workflowId, "Failed to resume transfer: " + e.getMessage());
        }
    }

    public TransferControlResponse cancelTransfer(String workflowId, String reason) {
        try {
            if (!isWorkflowActive(workflowId)) {
                log.warn("Cannot cancel - workflow is not active. WorkflowId: {}", workflowId);
                return TransferControlResponse.error(workflowId, "Workflow is not active or has been terminated");
            }

            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            workflow.cancelTransfer(reason != null ? reason : "Cancelled by user");

            TransferControlStatus status = workflow.getControlStatus();

            log.info("Transfer cancelled successfully. WorkflowId: {}, Reason: {}", workflowId, reason);
            return TransferControlResponse.success(workflowId, status, "Transfer cancelled successfully");

        } catch (Exception e) {
            log.error("Error cancelling transfer for workflowId {}: {}", workflowId, e.getMessage());
            return TransferControlResponse.error(workflowId, "Failed to cancel transfer: " + e.getMessage());
        }
    }

    public TransferControlStatus getControlStatus(String workflowId) {
        try {
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);

            return workflow.getControlStatus();

        } catch (Exception e) {
            log.error("Error getting control status for workflowId {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to get control status: " + e.getMessage());
        }
    }
}
