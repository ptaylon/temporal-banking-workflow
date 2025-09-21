package com.example.temporal.transfer.controller;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.initiateTransfer(request));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<TransferResponse> getTransferStatus(@PathVariable String workflowId) {
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