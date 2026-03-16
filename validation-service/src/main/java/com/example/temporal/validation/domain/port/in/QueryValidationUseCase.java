package com.example.temporal.validation.domain.port.in;

import com.example.temporal.validation.domain.model.TransferValidationDomain;

import java.util.List;
import java.util.Optional;

/**
 * Use case for querying validations
 * Defines what the system can do regarding validation queries
 */
public interface QueryValidationUseCase {

    /**
     * Gets a validation by ID
     * @param validationId the validation ID
     * @return the validation if found
     */
    Optional<TransferValidationDomain> getValidationById(Long validationId);

    /**
     * Gets validations by transfer ID
     * @param transferId the transfer ID
     * @return list of validations for the transfer
     */
    List<TransferValidationDomain> getValidationsByTransferId(String transferId);

    /**
     * Gets validations by account number
     * @param accountNumber the account number
     * @return list of validations involving the account
     */
    List<TransferValidationDomain> getValidationsByAccount(String accountNumber);

    /**
     * Gets pending validations
     * @return list of pending validations
     */
    List<TransferValidationDomain> getPendingValidations();
}
