package com.example.temporal.transfer.infrastructure.adapter.out.http;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.exception.ValidationException;
import com.example.temporal.transfer.client.ValidationServiceClient;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.out.ValidationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter for validation service operations
 * Implements domain port using Feign client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationAdapter implements ValidationPort {

    private final ValidationServiceClient validationServiceClient;

    @Override
    public void validateTransfer(TransferDomain transfer) {
        log.debug("Validating transfer: {}", transfer.getId());

        try {
            // Convert domain to DTO for validation service
            TransferRequest request = toTransferRequest(transfer);

            // Call validation service
            validationServiceClient.validateTransfer(request);

            log.debug("Transfer validation successful for ID: {}", transfer.getId());

        } catch (Exception e) {
            log.warn("Transfer validation failed for ID: {} - Error: {}", transfer.getId(), e.getMessage());

            // Wrap in domain exception
            if (isBusinessValidationError(e)) {
                throw new ValidationException("Validation failed: " + e.getMessage(), e);
            } else {
                // Temporary error - let it propagate for retry
                throw e;
            }
        }
    }

    /**
     * Check if error is a business validation error (should not retry)
     */
    private boolean isBusinessValidationError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("insufficient funds") ||
               message.contains("invalid account") ||
               message.contains("account not found") ||
               message.contains("invalid amount") ||
               message.contains("validation failed");
    }

    /**
     * Convert domain model to DTO for validation service
     */
    private TransferRequest toTransferRequest(TransferDomain transfer) {
        TransferRequest request = new TransferRequest();
        request.setTransferId(transfer.getId());
        request.setSourceAccountNumber(transfer.getSourceAccountNumber());
        request.setDestinationAccountNumber(transfer.getDestinationAccountNumber());
        request.setAmount(transfer.getAmount());
        request.setCurrency(transfer.getCurrency());
        return request;
    }
}
