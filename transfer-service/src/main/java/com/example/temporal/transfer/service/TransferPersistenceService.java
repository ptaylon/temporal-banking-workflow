package com.example.temporal.transfer.service;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.model.Transfer;
import com.example.temporal.common.model.TransferStatus;
import com.example.temporal.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferPersistenceService {

    private final TransferRepository transferRepository;

    @Transactional
    public Transfer createTransfer(TransferRequest request) {
        log.info("Creating transfer record for request: {}", request);
        
        Transfer transfer = new Transfer()
                .setSourceAccountNumber(request.getSourceAccountNumber())
                .setDestinationAccountNumber(request.getDestinationAccountNumber())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setStatus(TransferStatus.INITIATED);

        Transfer savedTransfer = transferRepository.save(transfer);
        log.info("Transfer record created with ID: {}", savedTransfer.getId());
        
        return savedTransfer;
    }

    @Transactional
    public Transfer updateTransferStatus(Long transferId, TransferStatus status) {
        log.info("Updating transfer {} status to {}", transferId, status);
        
        Optional<Transfer> transferOpt = transferRepository.findById(transferId);
        if (transferOpt.isPresent()) {
            Transfer transfer = transferOpt.get();
            transfer.setStatus(status);
            Transfer updatedTransfer = transferRepository.save(transfer);
            log.info("Transfer {} status updated to {}", transferId, status);
            return updatedTransfer;
        } else {
            log.warn("Transfer with ID {} not found", transferId);
            throw new RuntimeException("Transfer not found: " + transferId);
        }
    }

    @Transactional
    public Transfer updateTransferStatus(Long transferId, TransferStatus status, String failureReason) {
        log.info("Updating transfer {} status to {} with reason: {}", transferId, status, failureReason);
        
        Optional<Transfer> transferOpt = transferRepository.findById(transferId);
        if (transferOpt.isPresent()) {
            Transfer transfer = transferOpt.get();
            transfer.setStatus(status);
            // Truncate failure reason to fit database column limit (255 characters)
            String truncatedReason = failureReason != null && failureReason.length() > 255 
                ? failureReason.substring(0, 252) + "..." 
                : failureReason;
            transfer.setFailureReason(truncatedReason);
            Transfer updatedTransfer = transferRepository.save(transfer);
            log.info("Transfer {} status updated to {} with reason", transferId, status);
            return updatedTransfer;
        } else {
            log.warn("Transfer with ID {} not found", transferId);
            throw new RuntimeException("Transfer not found: " + transferId);
        }
    }

    public Optional<Transfer> findById(Long transferId) {
        return transferRepository.findById(transferId);
    }

    public List<Transfer> findByAccountNumber(String accountNumber) {
        return transferRepository.findByAccountNumber(accountNumber);
    }

    public List<Transfer> findByStatus(TransferStatus status) {
        return transferRepository.findByStatus(status);
    }
}