package com.example.temporal.validation.service;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.validation.client.AccountServiceClient;
import com.example.temporal.common.exception.ValidationException;
import com.example.temporal.validation.model.FraudDetectionRule;
import com.example.temporal.validation.repository.FraudDetectionRuleRepository;
import com.example.temporal.validation.repository.TransferLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final TransferLimitRepository transferLimitRepository;
    private final FraudDetectionRuleRepository fraudRuleRepository;
    private final AccountServiceClient accountService;

    @Transactional(readOnly = true)
    public void validateTransfer(final TransferRequest request) {
        log.info("Validating transfer request: {}", request);

        validateAccounts(request);
        validateTransferLimits(request);
        validateFraudRules(request);

        log.info("Transfer validation successful");
    }

    private void validateAccounts(final TransferRequest request) {
        var sourceAccount = accountService.getAccount(request.getSourceAccountNumber());
        var destAccount = accountService.getAccount(request.getDestinationAccountNumber());

        if (!sourceAccount.getCurrency().equals(request.getCurrency())) {
            throw new ValidationException("Source account currency does not match transfer currency");
        }

        if (!destAccount.getCurrency().equals(request.getCurrency())) {
            throw new ValidationException("Destination account currency does not match transfer currency");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ValidationException("Insufficient funds in source account");
        }
    }

    private void validateTransferLimits(TransferRequest request) {
        // TODO validations
//        var transferLimit = transferLimitRepository
//                .findByAccountTypeAndCurrency("STANDARD", request.getCurrency())
//                .orElseThrow(() -> new ValidationException("Transfer limits not configured"));
//
//        if (request.getAmount().compareTo(transferLimit.getSingleTransferLimit()) > 0) {
//            throw new ValidationException("Transfer amount exceeds single transfer limit");
//        }

        // In a real application, we would check daily and monthly aggregates
        // This is simplified for the demo
    }

    private void validateFraudRules(TransferRequest request) {
        List<FraudDetectionRule> activeRules = fraudRuleRepository.findByIsActiveTrue();

        for (FraudDetectionRule rule : activeRules) {
            switch (rule.getRuleType()) {
                case "AMOUNT_THRESHOLD":
                    if (request.getAmount().compareTo(rule.getThreshold()) > 0) {
                        throw new ValidationException("Transfer amount exceeds fraud detection threshold");
                    }
                    break;
                case "FREQUENCY":
                    // In a real application, we would check transfer frequency
                    // This is simplified for the demo
                    break;
                case "LOCATION":
                    // In a real application, we would check geographical location
                    // This is simplified for the demo
                    break;
                default:
                    log.warn("Unknown fraud rule type: {}", rule.getRuleType());
            }
        }
    }
}