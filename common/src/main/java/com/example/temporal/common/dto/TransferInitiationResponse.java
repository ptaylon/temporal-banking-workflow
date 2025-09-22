package com.example.temporal.common.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class TransferInitiationResponse {
    private Long transferId;
    private String workflowId;
    private String status;
    private String message;
    private LocalDateTime initiatedAt;
    
    public static TransferInitiationResponse success(Long transferId, String workflowId) {
        return new TransferInitiationResponse()
                .setTransferId(transferId)
                .setWorkflowId(workflowId)
                .setStatus("INITIATED")
                .setMessage("Transfer initiated successfully. Use the transferId to check status.")
                .setInitiatedAt(LocalDateTime.now());
    }
    
    public static TransferInitiationResponse error(String message) {
        return new TransferInitiationResponse()
                .setStatus("ERROR")
                .setMessage(message)
                .setInitiatedAt(LocalDateTime.now());
    }
}