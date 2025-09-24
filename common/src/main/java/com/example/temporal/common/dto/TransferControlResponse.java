package com.example.temporal.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/**
 * DTO para resposta de controle de transferência
 */
@Data
@Accessors(chain = true)
public class TransferControlResponse {
    
    private String workflowId;
    private TransferControlStatus status;
    private String message;
    private LocalDateTime timestamp;
    private boolean success;
    
    public static TransferControlResponse success(String workflowId, TransferControlStatus status, String message) {
        return new TransferControlResponse()
                .setWorkflowId(workflowId)
                .setStatus(status)
                .setMessage(message)
                .setSuccess(true)
                .setTimestamp(LocalDateTime.now());
    }
    
    public static TransferControlResponse error(String workflowId, String message) {
        return new TransferControlResponse()
                .setWorkflowId(workflowId)
                .setMessage(message)
                .setSuccess(false)
                .setTimestamp(LocalDateTime.now());
    }
}