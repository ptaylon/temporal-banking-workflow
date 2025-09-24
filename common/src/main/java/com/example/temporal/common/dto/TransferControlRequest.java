package com.example.temporal.common.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para requisições de controle de transferência
 */
@Data
public class TransferControlRequest {
    
    @NotNull(message = "Action is required")
    private TransferControlAction action;
    
    private String reason;
    
    private String workflowId;
}