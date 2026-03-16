package com.example.temporal.validation.repository;

import com.example.temporal.validation.entity.TransferValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for transfer validation entities
 */
@Repository
public interface TransferValidationRepository extends JpaRepository<TransferValidationEntity, Long> {

    /**
     * Finds a validation by idempotency key
     */
    Optional<TransferValidationEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds validations by transfer ID
     */
    List<TransferValidationEntity> findByTransferId(String transferId);

    /**
     * Finds validations by source or destination account
     */
    List<TransferValidationEntity> findBySourceAccountNumberOrDestinationAccountNumber(
            String sourceAccountNumber, String destinationAccountNumber);

    /**
     * Finds validations by validation result
     */
    List<TransferValidationEntity> findByValidationResult(
            TransferValidationEntity.ValidationResult validationResult);
}
