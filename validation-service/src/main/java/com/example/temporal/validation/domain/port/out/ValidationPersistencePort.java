package com.example.temporal.validation.domain.port.out;

import com.example.temporal.validation.domain.model.TransferValidationDomain;

import java.util.List;
import java.util.Optional;

/**
 * Port for validation persistence operations
 * Defines what the domain needs from the infrastructure
 */
public interface ValidationPersistencePort {

    /**
     * Saves a validation
     * @param validation the validation to save
     * @return the saved validation
     */
    TransferValidationDomain save(TransferValidationDomain validation);

    /**
     * Finds a validation by ID
     * @param id the validation ID
     * @return the validation if found
     */
    Optional<TransferValidationDomain> findById(Long id);

    /**
     * Finds a validation by idempotency key
     * @param idempotencyKey the idempotency key
     * @return the validation if found
     */
    Optional<TransferValidationDomain> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds validations by transfer ID
     * @param transferId the transfer ID
     * @return list of validations
     */
    List<TransferValidationDomain> findByTransferId(String transferId);

    /**
     * Finds validations by account number (source or destination)
     * @param accountNumber the account number
     * @return list of validations
     */
    List<TransferValidationDomain> findByAccountNumber(String accountNumber);

    /**
     * Finds pending validations
     * @return list of pending validations
     */
    List<TransferValidationDomain> findPendingValidations();
}
