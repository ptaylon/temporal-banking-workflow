package com.example.temporal.validation.domain.service;

import com.example.temporal.validation.domain.model.TransferValidationDomain;
import com.example.temporal.validation.domain.port.in.ValidateTransferUseCase;
import com.example.temporal.validation.domain.port.in.QueryValidationUseCase;
import com.example.temporal.validation.domain.port.out.AccountServicePort;
import com.example.temporal.validation.domain.port.out.FraudRulePort;
import com.example.temporal.validation.domain.port.out.TransferLimitPort;
import com.example.temporal.validation.domain.port.out.ValidationPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service implementing transfer validation use cases
 * Contains pure business logic without framework dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService implements ValidateTransferUseCase, QueryValidationUseCase {

    private final ValidationPersistencePort validationPersistencePort;
    private final AccountServicePort accountServicePort;
    private final TransferLimitPort transferLimitPort;
    private final FraudRulePort fraudRulePort;

    @Override
    @Transactional
    public ValidateTransferUseCase.ValidationResult validateTransfer(ValidateTransferCommand command) {
        log.info("Validating transfer: {}", command);

        try {
            // Check for duplicate validation (idempotency)
            String idempotencyKey = command.idempotencyKey() != null
                    ? command.idempotencyKey()
                    : UUID.randomUUID().toString();

            var existingValidation = validationPersistencePort.findByIdempotencyKey(idempotencyKey);
            if (existingValidation.isPresent()) {
                log.info("Validation already exists for idempotency key: {}", idempotencyKey);
                var validation = existingValidation.get();
                return ValidateTransferUseCase.ValidationResult.approved(
                        validation.getId(), 
                        validation.getFraudScore()
                );
            }

            // Create pending validation
            TransferValidationDomain validation = TransferValidationDomain.createPending(
                    command.sourceAccountNumber(),
                    command.destinationAccountNumber(),
                    command.amount(),
                    command.currency(),
                    idempotencyKey
            );

            // Perform validations
            validateAccounts(command);
            var limitValidationResult = validateTransferLimits(command);
            var fraudValidationResult = validateFraudRules(command);

            // Update validation based on results
            if (!limitValidationResult.approved) {
                validation = validation.reject(limitValidationResult.reason);
                validationPersistencePort.save(validation);
                return ValidateTransferUseCase.ValidationResult.rejected(
                        validation.getId(), 
                        limitValidationResult.reason
                );
            }

            if (!fraudValidationResult.approved) {
                validation = validation.reject(fraudValidationResult.reason)
                        .withFraudScore(fraudValidationResult.fraudScore);
                validationPersistencePort.save(validation);
                return ValidateTransferUseCase.ValidationResult.rejected(
                        validation.getId(), 
                        fraudValidationResult.reason
                );
            }

            // All validations passed - approve
            validation = validation.approve()
                    .withFraudScore(fraudValidationResult.fraudScore);
            TransferValidationDomain savedValidation = validationPersistencePort.save(validation);

            log.info("Transfer validation approved with ID: {}", savedValidation.getId());

            return ValidateTransferUseCase.ValidationResult.approved(
                    savedValidation.getId(), 
                    fraudValidationResult.fraudScore
            );

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ValidateTransferUseCase.ValidationResult.rejected(null, e.getMessage());
        } catch (Exception e) {
            log.error("Error validating transfer: {}", e.getMessage(), e);
            return ValidateTransferUseCase.ValidationResult.rejected(null, "Validation failed: " + e.getMessage());
        }
    }

    /**
     * Validates account-related checks
     */
    private void validateAccounts(ValidateTransferCommand command) {
        log.debug("Validating accounts for transfer");

        // Check source account exists and is valid
        var sourceAccount = accountServicePort.getAccount(command.sourceAccountNumber());
        if (sourceAccount.isEmpty()) {
            throw new IllegalArgumentException("Source account does not exist: " + command.sourceAccountNumber());
        }

        // Check destination account exists and is valid
        var destAccount = accountServicePort.getAccount(command.destinationAccountNumber());
        if (destAccount.isEmpty()) {
            throw new IllegalArgumentException("Destination account does not exist: " + command.destinationAccountNumber());
        }

        // Check currencies match
        if (!sourceAccount.get().currency().equals(command.currency())) {
            throw new IllegalArgumentException(
                    "Source account currency (" + sourceAccount.get().currency() + 
                    ") does not match transfer currency (" + command.currency() + ")");
        }

        if (!destAccount.get().currency().equals(command.currency())) {
            throw new IllegalArgumentException(
                    "Destination account currency (" + destAccount.get().currency() + 
                    ") does not match transfer currency (" + command.currency() + ")");
        }

        // Check sufficient funds
        if (sourceAccount.get().balance().compareTo(command.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account");
        }

        // Check accounts are not the same
        if (command.sourceAccountNumber().equals(command.destinationAccountNumber())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        log.debug("Account validation passed");
    }

    /**
     * Validates transfer limits
     * @return validation result with reason if rejected
     */
    private InternalValidationResult validateTransferLimits(ValidateTransferCommand command) {
        log.debug("Validating transfer limits");

        // Get transfer limit for account type (using STANDARD as default for demo)
        var limitInfo = transferLimitPort.getByAccountTypeAndCurrency("STANDARD", command.currency());

        if (limitInfo.isPresent()) {
            var limit = limitInfo.get();
            
            // Check single transfer limit
            if (command.amount().compareTo(limit.singleTransferLimit()) > 0) {
                String reason = "Transfer amount (" + command.amount() + 
                               ") exceeds single transfer limit (" + limit.singleTransferLimit() + ")";
                log.warn("Transfer limit validation failed: {}", reason);
                return new InternalValidationResult(false, reason, null);
            }

            // Note: Daily and monthly limits would require querying historical transfers
            // This is simplified for the demo
        } else {
            log.warn("Transfer limit not configured for STANDARD/{}", command.currency());
            // Don't reject - allow transfer if limits not configured (demo behavior)
        }

        log.debug("Transfer limit validation passed");
        return new InternalValidationResult(true, null, null);
    }

    /**
     * Validates fraud rules
     * @return validation result with fraud score
     */
    private InternalValidationResult validateFraudRules(ValidateTransferCommand command) {
        log.debug("Validating fraud rules");

        var activeRules = fraudRulePort.getActiveRules();
        int fraudScore = 0;

        for (var rule : activeRules) {
            switch (rule.ruleType()) {
                case "AMOUNT_THRESHOLD" -> {
                    if (command.amount().compareTo(rule.threshold()) > 0) {
                        fraudScore += 50; // High amount increases fraud score
                        log.warn("Transfer amount exceeds fraud threshold: {} > {}", 
                                command.amount(), rule.threshold());
                    }
                }
                case "FREQUENCY" -> {
                    // In a real application, check transfer frequency
                    // This is simplified for the demo
                    log.debug("Frequency check not implemented in demo");
                }
                case "LOCATION" -> {
                    // In a real application, check geographical location
                    // This is simplified for the demo
                    log.debug("Location check not implemented in demo");
                }
                default -> log.warn("Unknown fraud rule type: {}", rule.ruleType());
            }
        }

        // Reject if fraud score is too high (threshold: 75)
        if (fraudScore >= 75) {
            String reason = "High fraud score: " + fraudScore;
            log.warn("Transfer rejected due to high fraud score: {}", fraudScore);
            return new InternalValidationResult(false, reason, fraudScore);
        }

        log.debug("Fraud validation passed with score: {}", fraudScore);
        return new InternalValidationResult(true, null, fraudScore);
    }

    /**
     * Internal validation result for sub-validations
     */
    private record InternalValidationResult(
            boolean approved,
            String reason,
            Integer fraudScore
    ) {}

    // ========== QueryValidationUseCase Implementation ==========

    @Override
    @Transactional(readOnly = true)
    public Optional<TransferValidationDomain> getValidationById(Long validationId) {
        log.debug("Getting validation by ID: {}", validationId);
        return validationPersistencePort.findById(validationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferValidationDomain> getValidationsByTransferId(String transferId) {
        log.debug("Getting validations by transfer ID: {}", transferId);
        return validationPersistencePort.findByTransferId(transferId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferValidationDomain> getValidationsByAccount(String accountNumber) {
        log.debug("Getting validations by account: {}", accountNumber);
        return validationPersistencePort.findByAccountNumber(accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferValidationDomain> getPendingValidations() {
        log.debug("Getting pending validations");
        return validationPersistencePort.findPendingValidations();
    }
}
