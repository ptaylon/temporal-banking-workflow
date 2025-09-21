package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WorkflowClient workflowClient;

    public TransferResponse initiateTransfer(TransferRequest request) {
        String workflowId = UUID.randomUUID().toString();
        
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(MoneyTransferWorkflow.QUEUE_NAME)
                .setWorkflowId(workflowId)
                .setWorkflowTaskTimeout(java.time.Duration.ofMinutes(1))
                .setWorkflowRunTimeout(java.time.Duration.ofMinutes(10))
                .build();

        MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                MoneyTransferWorkflow.class, options);

        return workflow.executeTransfer(request);
    }

    public TransferResponse getTransferStatus(String workflowId) {
        // Consulta o status atual via método @Query do workflow
        MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                MoneyTransferWorkflow.class, workflowId);
        return workflow.getStatus();
    }
}