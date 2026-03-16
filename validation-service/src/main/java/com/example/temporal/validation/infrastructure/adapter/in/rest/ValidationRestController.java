package com.example.temporal.validation.infrastructure.adapter.in.rest;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.validation.domain.port.in.ValidateTransferUseCase;
import com.example.temporal.validation.domain.port.in.QueryValidationUseCase;
import com.example.temporal.validation.domain.model.TransferValidationDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller adapter for validation operations
 * Exposes domain use cases as HTTP endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationRestController {

    private final ValidateTransferUseCase validateTransferUseCase;
    private final QueryValidationUseCase queryValidationUseCase;

    /**
     * Validates a transfer request
     */
    @PostMapping
    public ResponseEntity<ValidationResponse> validateTransfer(
            @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("REST API: Validating transfer for {}", request.getSourceAccountNumber());

        var command = ValidateTransferUseCase.ValidateTransferCommand.of(
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber(),
                request.getAmount(),
                request.getCurrency(),
                idempotencyKey
        );

        var result = validateTransferUseCase.validateTransfer(command);

        var response = new ValidationResponse(
                result.validationId(),
                result.approved(),
                result.rejectionReason(),
                result.fraudScore()
        );

        return result.approved() 
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Gets validation by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ValidationDetailResponse> getValidationById(@PathVariable Long id) {
        log.info("REST API: Getting validation by ID: {}", id);

        Optional<TransferValidationDomain> validation = queryValidationUseCase.getValidationById(id);

        return validation.map(v -> ResponseEntity.ok(toDetailResponse(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets validations by transfer ID
     */
    @GetMapping("/transfer/{transferId}")
    public ResponseEntity<List<ValidationSummaryResponse>> getValidationsByTransferId(
            @PathVariable String transferId) {
        log.info("REST API: Getting validations by transfer ID: {}", transferId);

        List<TransferValidationDomain> validations = 
                queryValidationUseCase.getValidationsByTransferId(transferId);

        return ResponseEntity.ok(validations.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets validations by account number
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<ValidationSummaryResponse>> getValidationsByAccount(
            @PathVariable String accountNumber) {
        log.info("REST API: Getting validations by account: {}", accountNumber);

        List<TransferValidationDomain> validations = 
                queryValidationUseCase.getValidationsByAccount(accountNumber);

        return ResponseEntity.ok(validations.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Gets pending validations
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ValidationSummaryResponse>> getPendingValidations() {
        log.info("REST API: Getting pending validations");

        List<TransferValidationDomain> validations = 
                queryValidationUseCase.getPendingValidations();

        return ResponseEntity.ok(validations.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
    }

    private ValidationDetailResponse toDetailResponse(TransferValidationDomain v) {
        return new ValidationDetailResponse(
                v.getId(),
                v.getTransferId(),
                v.getSourceAccountNumber(),
                v.getDestinationAccountNumber(),
                v.getAmount(),
                v.getCurrency(),
                v.getValidationResult().name(),
                v.getRejectionReason(),
                v.getFraudScore(),
                v.getValidatedAt(),
                v.getIdempotencyKey()
        );
    }

    private ValidationSummaryResponse toSummaryResponse(TransferValidationDomain v) {
        return new ValidationSummaryResponse(
                v.getId(),
                v.getTransferId(),
                v.getSourceAccountNumber(),
                v.getDestinationAccountNumber(),
                v.getAmount(),
                v.getValidationResult().name(),
                v.getValidatedAt()
        );
    }

    // Response DTOs

    public record ValidationResponse(
            Long validationId,
            Boolean approved,
            String rejectionReason,
            Integer fraudScore
    ) {}

    public record ValidationDetailResponse(
            Long id,
            String transferId,
            String sourceAccountNumber,
            String destinationAccountNumber,
            java.math.BigDecimal amount,
            String currency,
            String validationResult,
            String rejectionReason,
            Integer fraudScore,
            java.time.LocalDateTime validatedAt,
            String idempotencyKey
    ) {}

    public record ValidationSummaryResponse(
            Long id,
            String transferId,
            String sourceAccountNumber,
            String destinationAccountNumber,
            java.math.BigDecimal amount,
            String validationResult,
            java.time.LocalDateTime validatedAt
    ) {}
}
