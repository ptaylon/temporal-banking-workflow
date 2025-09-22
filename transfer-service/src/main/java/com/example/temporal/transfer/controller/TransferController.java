package com.example.temporal.transfer.controller;

import com.example.temporal.common.dto.TransferInitiationResponse;
import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

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
}