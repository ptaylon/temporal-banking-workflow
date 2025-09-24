package com.example.temporal.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * DTO para status de controle de transferência
 */
@Data
@Accessors(chain = true)
public class TransferControlStatus {
    
    private boolean paused;
    private boolean cancelled;
    private String pauseReason;
    private String cancelReason;
    private TransferControlAction lastControlAction;
    private LocalDateTime lastControlTimestamp;
    private String workflowStatus;
}