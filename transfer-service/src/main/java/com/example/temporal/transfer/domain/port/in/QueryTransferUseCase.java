package com.example.temporal.transfer.domain.port.in;

import com.example.temporal.transfer.domain.model.TransferDomain;

import java.util.List;
import java.util.Optional;

/**
 * Input port (driving port) for querying transfers
 */
public interface QueryTransferUseCase {

    /**
     * Get transfer by ID
     */
    Optional<TransferDomain> getTransferById(Long transferId);

    /**
     * Get transfer by workflow ID
     */
    Optional<TransferDomain> getTransferByWorkflowId(String workflowId);

    /**
     * Get all transfers for an account
     */
    List<TransferDomain> getTransfersByAccount(String accountNumber);

    /**
     * Get transfer status including workflow state
     */
    TransferStatusResult getTransferStatus(String workflowId);

    @lombok.Value
    @lombok.Builder
    class TransferStatusResult {
        TransferDomain transfer;
        String workflowStatus;
        boolean isWorkflowRunning;
    }
}
