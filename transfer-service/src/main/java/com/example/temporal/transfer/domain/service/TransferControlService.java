package com.example.temporal.transfer.domain.service;

import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.in.ControlTransferUseCase;
import com.example.temporal.transfer.domain.port.out.TransferPersistencePort;
import com.example.temporal.transfer.domain.port.out.WorkflowOrchestrationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Domain service for controlling transfer workflows
 * Implements pause, resume, and cancel operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferControlService implements ControlTransferUseCase {

    private final TransferPersistencePort persistencePort;
    private final WorkflowOrchestrationPort orchestrationPort;

    @Override
    public ControlResult pauseTransfer(String workflowId) {
        log.info("Pausing transfer with workflowId: {}", workflowId);

        try {
            // Verify transfer exists and can be paused
            Optional<TransferDomain> transferOpt = getTransferByWorkflowId(workflowId);

            if (transferOpt.isEmpty()) {
                return ControlResult.failure(workflowId, "Transfer not found");
            }

            TransferDomain transfer = transferOpt.get();

            if (!transfer.canBePaused()) {
                return ControlResult.failure(workflowId,
                    "Transfer cannot be paused in status: " + transfer.getStatus());
            }

            // Pause workflow
            orchestrationPort.pauseWorkflow(workflowId);

            log.info("Transfer paused successfully: {}", workflowId);
            return ControlResult.success(workflowId, "Transfer paused successfully");

        } catch (Exception e) {
            log.error("Error pausing transfer {}: {}", workflowId, e.getMessage());
            return ControlResult.failure(workflowId, "Failed to pause transfer: " + e.getMessage());
        }
    }

    @Override
    public ControlResult resumeTransfer(String workflowId) {
        log.info("Resuming transfer with workflowId: {}", workflowId);

        try {
            // Verify transfer exists
            Optional<TransferDomain> transferOpt = getTransferByWorkflowId(workflowId);

            if (transferOpt.isEmpty()) {
                return ControlResult.failure(workflowId, "Transfer not found");
            }

            // Resume workflow
            orchestrationPort.resumeWorkflow(workflowId);

            log.info("Transfer resumed successfully: {}", workflowId);
            return ControlResult.success(workflowId, "Transfer resumed successfully");

        } catch (Exception e) {
            log.error("Error resuming transfer {}: {}", workflowId, e.getMessage());
            return ControlResult.failure(workflowId, "Failed to resume transfer: " + e.getMessage());
        }
    }

    @Override
    public ControlResult cancelTransfer(String workflowId, String reason) {
        log.info("Cancelling transfer with workflowId: {}, reason: {}", workflowId, reason);

        try {
            // Verify transfer exists and can be cancelled
            Optional<TransferDomain> transferOpt = getTransferByWorkflowId(workflowId);

            if (transferOpt.isEmpty()) {
                return ControlResult.failure(workflowId, "Transfer not found");
            }

            TransferDomain transfer = transferOpt.get();

            if (!transfer.canBeCancelled()) {
                return ControlResult.failure(workflowId,
                    "Transfer cannot be cancelled in status: " + transfer.getStatus());
            }

            // Cancel workflow
            orchestrationPort.cancelWorkflow(workflowId, reason);

            log.info("Transfer cancelled successfully: {}", workflowId);
            return ControlResult.success(workflowId, "Transfer cancelled successfully");

        } catch (Exception e) {
            log.error("Error cancelling transfer {}: {}", workflowId, e.getMessage());
            return ControlResult.failure(workflowId, "Failed to cancel transfer: " + e.getMessage());
        }
    }

    @Override
    public ControlStatusResult getControlStatus(String workflowId) {
        log.debug("Getting control status for workflowId: {}", workflowId);

        try {
            WorkflowOrchestrationPort.WorkflowStatus status = orchestrationPort.getWorkflowStatus(workflowId);

            return ControlStatusResult.builder()
                    .paused(status.isPaused())
                    .cancelled(status.isCancelled())
                    .pauseReason(status.getPauseReason())
                    .cancelReason(status.getCancelReason())
                    .workflowStatus(status.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Error getting control status for {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Failed to get control status: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to extract transfer from workflow ID
     */
    private Optional<TransferDomain> getTransferByWorkflowId(String workflowId) {
        try {
            Long transferId = Long.parseLong(workflowId.replace("transfer-", ""));
            return persistencePort.findById(transferId);
        } catch (NumberFormatException e) {
            log.error("Invalid workflow ID format: {}", workflowId);
            return Optional.empty();
        }
    }
}
