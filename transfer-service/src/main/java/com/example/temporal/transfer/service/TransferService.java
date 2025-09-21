package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.model.Transfer;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WorkflowClient workflowClient;
    private final TransferPersistenceService transferPersistenceService;

    public TransferResponse initiateTransfer(TransferRequest request) {
        log.info("Initiating transfer: {}", request);
        
        // 1. Primeiro, criar o registro da transferência no banco
        Transfer transfer = transferPersistenceService.createTransfer(request);
        
        // 2. Criar o workflow com o ID da transferência
        String workflowId = "transfer-" + transfer.getId();
        
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(MoneyTransferWorkflow.QUEUE_NAME)
                .setWorkflowId(workflowId)
                .setWorkflowTaskTimeout(java.time.Duration.ofMinutes(1))
                .setWorkflowRunTimeout(java.time.Duration.ofMinutes(10))
                .build();

        MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                MoneyTransferWorkflow.class, options);

        // 3. Definir o ID da transferência no request e executar o workflow
        request.setTransferId(transfer.getId());
        TransferResponse response = workflow.executeTransfer(request);
        
        // 4. Definir o ID da transferência na resposta
        response.setTransferId(transfer.getId());
        response.setCreatedAt(transfer.getCreatedAt());
        response.setUpdatedAt(transfer.getUpdatedAt());
        
        log.info("Transfer initiated with ID: {}", transfer.getId());
        return response;
    }

    public List<Transfer> getTransfersByAccount(String accountNumber) {
        return transferPersistenceService.findByAccountNumber(accountNumber);
    }

    public Transfer getTransferById(Long transferId) {
        return transferPersistenceService.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
    }

    public TransferResponse getTransferStatus(String workflowId) {
        try {
            // Extrair o ID da transferência do workflowId (formato: "transfer-123")
            Long transferId = Long.parseLong(workflowId.replace("transfer-", ""));
            
            // Buscar no banco de dados primeiro
            Transfer transfer = transferPersistenceService.findById(transferId)
                    .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
            
            // Consultar o status atual via workflow se necessário
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);
            TransferResponse workflowResponse = workflow.getStatus();
            
            // Combinar dados do banco com dados do workflow
            TransferResponse response = new TransferResponse()
                    .setTransferId(transfer.getId())
                    .setSourceAccountNumber(transfer.getSourceAccountNumber())
                    .setDestinationAccountNumber(transfer.getDestinationAccountNumber())
                    .setAmount(transfer.getAmount())
                    .setCurrency(transfer.getCurrency())
                    .setStatus(transfer.getStatus())
                    .setFailureReason(transfer.getFailureReason())
                    .setCreatedAt(transfer.getCreatedAt())
                    .setUpdatedAt(transfer.getUpdatedAt());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting transfer status for workflowId {}: {}", workflowId, e.getMessage());
            // Fallback para consulta apenas no workflow
            MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                    MoneyTransferWorkflow.class, workflowId);
            return workflow.getStatus();
        }
    }
}