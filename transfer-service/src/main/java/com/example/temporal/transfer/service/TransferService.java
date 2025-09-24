package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferInitiationResponse;
import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.model.Transfer;
import com.example.temporal.common.workflow.MoneyTransferWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final WorkflowClient workflowClient;
    private final TransferPersistenceService transferPersistenceService;

    public TransferInitiationResponse initiateTransferAsync(TransferRequest request) {
        log.info("Initiating async transfer: {}", request);
        
        try {
            // 1. Primeiro, criar o registro da transferência no banco
            Transfer transfer = transferPersistenceService.createTransfer(request);
            
            // 2. Criar o workflow com o ID da transferência
            String workflowId = "transfer-" + transfer.getId();
            
            // 3. Definir o ID da transferência no request
            request.setTransferId(transfer.getId());
            
            // 4. Iniciar o workflow de forma assíncrona
            executeTransferWorkflowAsync(request, workflowId);
            
            log.info("Transfer initiated asynchronously with ID: {} and workflowId: {}", transfer.getId(), workflowId);
            return TransferInitiationResponse.success(transfer.getId(), workflowId);
            
        } catch (Exception e) {
            log.error("Error initiating transfer: {}", e.getMessage(), e);
            return TransferInitiationResponse.error("Failed to initiate transfer: " + e.getMessage());
        }
    }
    
    @Async("transferExecutor")
    public CompletableFuture<Void> executeTransferWorkflowAsync(TransferRequest request, String workflowId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting workflow execution for transfer ID: {} with workflowId: {}", request.getTransferId(), workflowId);
                
                WorkflowOptions options = WorkflowOptions.newBuilder()
                        .setTaskQueue(MoneyTransferWorkflow.QUEUE_NAME)
                        .setWorkflowId(workflowId)
                        .setWorkflowTaskTimeout(java.time.Duration.ofMinutes(1))
                        .setWorkflowRunTimeout(java.time.Duration.ofHours(1)) // Aumentado para 1 hora
                        .build();

                MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                        MoneyTransferWorkflow.class, options);

                // Executar o workflow (isso pode demorar muito tempo)
                TransferResponse response = workflow.executeTransfer(request);
                
                log.info("Workflow completed for transfer ID: {} with status: {}", 
                    request.getTransferId(), response.getStatus());
                    
            } catch (io.temporal.failure.TerminatedFailure e) {
                log.warn("Workflow terminated externally for transfer ID: {} - Reason: {}", 
                    request.getTransferId(), e.getMessage());
                // Workflow foi terminado via Web UI ou externamente - não é erro crítico
            } catch (io.temporal.failure.CanceledFailure e) {
                log.info("Workflow cancelled for transfer ID: {} - Reason: {}", 
                    request.getTransferId(), e.getMessage());
                // Workflow foi cancelado via signal - comportamento esperado
            } catch (io.temporal.client.WorkflowFailedException e) {
                if (e.getCause() instanceof io.temporal.failure.TerminatedFailure) {
                    log.warn("Workflow terminated externally for transfer ID: {} - {}", 
                        request.getTransferId(), e.getMessage());
                } else {
                    log.error("Workflow failed for transfer ID: {} - {}", 
                        request.getTransferId(), e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("Unexpected error executing workflow for transfer ID: {} - {}", 
                    request.getTransferId(), e.getMessage(), e);
            }
        });
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
            
            // Buscar no banco de dados primeiro (fonte da verdade para dados persistidos)
            Transfer transfer = transferPersistenceService.findById(transferId)
                    .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));
            
            // Criar resposta baseada nos dados do banco
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
            
            // Tentar obter informações adicionais do workflow se estiver em execução
            try {
                MoneyTransferWorkflow workflow = workflowClient.newWorkflowStub(
                        MoneyTransferWorkflow.class, workflowId);
                TransferResponse workflowResponse = workflow.getStatus();
                
                // Se o workflow tem informações mais recentes, usar elas
                if (workflowResponse != null && workflowResponse.getStatus() != null) {
                    response.setStatus(workflowResponse.getStatus());
                    if (workflowResponse.getFailureReason() != null) {
                        response.setFailureReason(workflowResponse.getFailureReason());
                    }
                }
            } catch (Exception workflowException) {
                log.debug("Could not query workflow status (workflow may be completed): {}", workflowException.getMessage());
                // Não é um erro crítico - usar dados do banco
            }
            
            return response;
            
        } catch (NumberFormatException e) {
            log.error("Invalid workflowId format {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Invalid workflow ID format: " + workflowId);
        } catch (Exception e) {
            log.error("Error getting transfer status for workflowId {}: {}", workflowId, e.getMessage());
            throw new RuntimeException("Error retrieving transfer status: " + e.getMessage());
        }
    }

}