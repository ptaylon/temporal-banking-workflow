package com.example.temporal.transfer.domain.port.in;

import lombok.Builder;
import lombok.Value;

/**
 * Input port (driving port) for controlling transfers (pause, resume, cancel)
 */
public interface ControlTransferUseCase {

    /**
     * Pause a running transfer
     */
    ControlResult pauseTransfer(String workflowId);

    /**
     * Resume a paused transfer
     */
    ControlResult resumeTransfer(String workflowId);

    /**
     * Cancel a transfer with reason
     */
    ControlResult cancelTransfer(String workflowId, String reason);

    /**
     * Get control status of a transfer
     */
    ControlStatusResult getControlStatus(String workflowId);

    @Value
    @Builder
    class ControlResult {
        boolean success;
        String message;
        String workflowId;

        public static ControlResult success(String workflowId, String message) {
            return ControlResult.builder()
                    .success(true)
                    .workflowId(workflowId)
                    .message(message)
                    .build();
        }

        public static ControlResult failure(String workflowId, String message) {
            return ControlResult.builder()
                    .success(false)
                    .workflowId(workflowId)
                    .message(message)
                    .build();
        }
    }

    @Value
    @Builder
    class ControlStatusResult {
        boolean paused;
        boolean cancelled;
        String pauseReason;
        String cancelReason;
        String workflowStatus;
    }
}
