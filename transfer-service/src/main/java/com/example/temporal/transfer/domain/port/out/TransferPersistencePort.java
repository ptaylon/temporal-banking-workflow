package com.example.temporal.transfer.domain.port.out;

import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.transfer.domain.model.TransferDomain;

import java.util.List;
import java.util.Optional;

/**
 * Output port (driven port) for transfer persistence
 * Defines repository contract without exposing persistence technology
 */
public interface TransferPersistencePort {

    /**
     * Save a new transfer
     */
    TransferDomain save(TransferDomain transfer);

    /**
     * Update an existing transfer
     */
    TransferDomain update(TransferDomain transfer);

    /**
     * Find transfer by ID
     */
    Optional<TransferDomain> findById(Long id);

    /**
     * Find transfer by idempotency key
     */
    Optional<TransferDomain> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find transfers by account number (source or destination)
     */
    List<TransferDomain> findByAccountNumber(String accountNumber);

    /**
     * Find transfers by status
     */
    List<TransferDomain> findByStatus(TransferStatus status);

    /**
     * Check if transfer exists with idempotency key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Update transfer status by ID
     */
    void updateTransferStatus(Long transferId, TransferStatus status);

    /**
     * Update transfer status with failure reason by ID
     */
    void updateTransferStatusWithReason(Long transferId, TransferStatus status, String reason);
}
