package com.example.temporal.transfer.domain.port.out;

import com.example.temporal.common.exception.ValidationException;
import com.example.temporal.transfer.domain.model.TransferDomain;

/**
 * Output port (driven port) for transfer validation
 * Defines contract for validating transfers with external validation service
 */
public interface ValidationPort {

    /**
     * Validate a transfer request
     * @throws ValidationException if validation fails
     */
    void validateTransfer(TransferDomain transfer) throws ValidationException;
}
