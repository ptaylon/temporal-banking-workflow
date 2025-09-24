package com.example.temporal.transfer.controller;

import com.example.temporal.common.dto.TransferInitiationResponse;
import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.dto.TransferControlRequest;
import com.example.temporal.common.dto.TransferControlResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import com.example.temporal.transfer.service.TransferService;
import com.example.temporal.transfer.service.FeatureFlagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final FeatureFlagService featureFlagService;
    private final com.example.temporal.transfer.service.TransferControlService transferControlService;

    @PostMapping
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(@RequestBody TransferRequest request) {
        TransferInitiationResponse response = transferService.initiateTransferAsync(request);
        
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{transferId}/status")
    public ResponseEntity<TransferResponse> getTransferStatus(@PathVariable Long transferId) {
        String workflowId = "transfer-" + transferId;
        return ResponseEntity.ok(transferService.getTransferStatus(workflowId));
    }
    
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<TransferResponse> getTransferStatusByWorkflowId(@PathVariable String workflowId) {
        return ResponseEntity.ok(transferService.getTransferStatus(workflowId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<?> getTransfersByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transferService.getTransfersByAccount(accountNumber));
    }

    @GetMapping("/transfer/{transferId}")
    public ResponseEntity<?> getTransferById(@PathVariable Long transferId) {
        return ResponseEntity.ok(transferService.getTransferById(transferId));
    }

    // ========== ENDPOINTS DE CONTROLE DE TRANSFERÊNCIAS ==========
    
    @PostMapping("/{workflowId}/pause")
    public ResponseEntity<?> pauseTransfer(@PathVariable String workflowId) {
        
        log.info("Request to pause transfer with workflowId: {}", workflowId);
        
        // Verificar se a funcionalidade está habilitada
        if (!featureFlagService.isControlEnabled()) {
            log.warn("Pause functionality is disabled - rejecting request for workflowId: {}", workflowId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Control functionality is temporarily disabled",
                        "workflowId", workflowId,
                        "feature", "control",
                        "enabled", false
                    ));
        }
        
        try {
            TransferControlResponse response = transferControlService.pauseTransfer(workflowId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error pausing transfer {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage(),
                        "workflowId", workflowId
                    ));
        }
    }

    @PostMapping("/{workflowId}/resume")
    public ResponseEntity<?> resumeTransfer(@PathVariable String workflowId) {
        
        log.info("Request to resume transfer with workflowId: {}", workflowId);
        
        // Verificar se a funcionalidade está habilitada
        if (!featureFlagService.isControlEnabled()) {
            log.warn("Resume functionality is disabled - rejecting request for workflowId: {}", workflowId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Control functionality is temporarily disabled",
                        "workflowId", workflowId,
                        "feature", "control",
                        "enabled", false
                    ));
        }
        
        try {
            TransferControlResponse response = transferControlService.resumeTransfer(workflowId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error resuming transfer {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage(),
                        "workflowId", workflowId
                    ));
        }
    }

    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<?> cancelTransfer(
            @PathVariable String workflowId,
            @RequestBody @Valid TransferControlRequest request) {
        
        log.info("Request to cancel transfer with workflowId: {}, reason: {}", workflowId, request.getReason());
        
        // Verificar se a funcionalidade está habilitada
        if (!featureFlagService.isControlEnabled()) {
            log.warn("Cancel functionality is disabled - rejecting request for workflowId: {}", workflowId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Control functionality is temporarily disabled",
                        "workflowId", workflowId,
                        "feature", "control",
                        "enabled", false
                    ));
        }
        
        try {
            String reason = request.getReason() != null ? request.getReason() : "Cancelled by user request";
            TransferControlResponse response = transferControlService.cancelTransfer(workflowId, reason);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error cancelling transfer {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage(),
                        "workflowId", workflowId
                    ));
        }
    }

    @GetMapping("/{workflowId}/control-status")
    public ResponseEntity<?> getControlStatus(@PathVariable String workflowId) {
        
        log.debug("Request to get control status for workflowId: {}", workflowId);
        
        try {
            TransferControlStatus status = transferControlService.getControlStatus(workflowId);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error getting control status for {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "error", "Workflow not found or not accessible",
                        "message", e.getMessage(),
                        "workflowId", workflowId
                    ));
        }
    }

    // ========== ENDPOINTS DE CONTROLE EM LOTE ==========
    
    @PostMapping("/batch/pause")
    public ResponseEntity<?> pauseMultipleTransfers(@RequestBody List<String> workflowIds) {
        
        log.info("Request to pause {} transfers", workflowIds.size());
        
        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Control functionality is temporarily disabled"));
        }
        
        // Implementação simplificada - em produção, usar processamento paralelo
        var results = workflowIds.stream()
                .map(workflowId -> {
                    try {
                        TransferControlResponse response = transferControlService.pauseTransfer(workflowId);
                        return Map.of(
                            "workflowId", workflowId,
                            "success", response.isSuccess(),
                            "message", response.getMessage()
                        );
                    } catch (Exception e) {
                        return Map.of(
                            "workflowId", workflowId,
                            "success", false,
                            "message", e.getMessage()
                        );
                    }
                })
                .toList();
        
        long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
        
        return ResponseEntity.ok(Map.of(
            "total", workflowIds.size(),
            "successful", successCount,
            "failed", workflowIds.size() - successCount,
            "results", results
        ));
    }

    @PostMapping("/batch/cancel")
    public ResponseEntity<?> cancelMultipleTransfers(@RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<String> workflowIds = (List<String>) request.get("workflowIds");
        String reason = (String) request.getOrDefault("reason", "Batch cancellation");
        
        log.info("Request to cancel {} transfers with reason: {}", workflowIds.size(), reason);
        
        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Control functionality is temporarily disabled"));
        }
        
        var results = workflowIds.stream()
                .map(workflowId -> {
                    try {
                        TransferControlResponse response = transferControlService.cancelTransfer(workflowId, reason);
                        return Map.of(
                            "workflowId", workflowId,
                            "success", response.isSuccess(),
                            "message", response.getMessage()
                        );
                    } catch (Exception e) {
                        return Map.of(
                            "workflowId", workflowId,
                            "success", false,
                            "message", e.getMessage()
                        );
                    }
                })
                .toList();
        
        long successCount = results.stream().mapToLong(r -> (Boolean) r.get("success") ? 1 : 0).sum();
        
        return ResponseEntity.ok(Map.of(
            "total", workflowIds.size(),
            "successful", successCount,
            "failed", workflowIds.size() - successCount,
            "reason", reason,
            "results", results
        ));
    }
}