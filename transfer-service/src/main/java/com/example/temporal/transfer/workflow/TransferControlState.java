package com.example.temporal.transfer.workflow;

import com.example.temporal.common.dto.TransferControlAction;
import io.temporal.workflow.Workflow;

import java.time.LocalDateTime;

/**
 * Manages workflow control state (pause, resume, cancel).
 * Encapsulates all state related to transfer control operations.
 */
final class TransferControlState {

    private boolean paused;
    private boolean cancelled;
    private String pauseReason;
    private String cancelReason;
    private TransferControlAction lastControlAction;
    private LocalDateTime lastControlTimestamp;
    private boolean delayCompleted;
    private boolean delayCancelled;

    TransferControlState() {
        this.paused = false;
        this.cancelled = false;
        this.delayCompleted = false;
        this.delayCancelled = false;
    }

    boolean isPaused() {
        return paused;
    }

    boolean isCancelled() {
        return cancelled;
    }

    boolean isDelayCompleted() {
        return delayCompleted;
    }

    boolean isDelayCancelled() {
        return delayCancelled;
    }

    TransferControlAction getLastControlAction() {
        return lastControlAction;
    }

    LocalDateTime getLastControlTimestamp() {
        return lastControlTimestamp;
    }

    String getPauseReason() {
        return pauseReason;
    }

    String getCancelReason() {
        return cancelReason;
    }

    void pause(final String reason) {
        this.paused = true;
        this.lastControlAction = TransferControlAction.PAUSE;
        this.lastControlTimestamp = LocalDateTime.now();
        this.pauseReason = reason;

        Workflow.getLogger(TransferControlState.class)
                .info("Transfer paused via signal. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
    }

    void resume() {
        this.paused = false;
        this.lastControlAction = TransferControlAction.RESUME;
        this.lastControlTimestamp = LocalDateTime.now();

        Workflow.getLogger(TransferControlState.class)
                .info("Transfer resumed via signal. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
    }

    void cancel(final String reason) {
        this.cancelled = true;
        this.lastControlAction = TransferControlAction.CANCEL;
        this.lastControlTimestamp = LocalDateTime.now();
        this.cancelReason = reason;

        Workflow.getLogger(TransferControlState.class)
                .info("Transfer cancelled via signal. WorkflowId: {}", Workflow.getInfo().getWorkflowId());
    }

    void markDelayCompleted() {
        this.delayCompleted = true;
    }

    void markDelayCancelled() {
        this.delayCancelled = true;
    }

    void reset() {
        this.paused = false;
        this.cancelled = false;
        this.pauseReason = null;
        this.cancelReason = null;
        this.delayCompleted = false;
        this.delayCancelled = false;
    }
}
