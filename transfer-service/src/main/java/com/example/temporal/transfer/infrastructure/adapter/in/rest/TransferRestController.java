package com.example.temporal.transfer.infrastructure.adapter.in.rest;

import com.example.temporal.common.dto.ErrorResponse;
import com.example.temporal.common.dto.TransferInitiationResponse;
import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.dto.TransferControlRequest;
import com.example.temporal.common.dto.TransferControlResponse;
import com.example.temporal.common.dto.BatchOperationResponse;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.in.ControlTransferUseCase;
import com.example.temporal.transfer.domain.port.in.InitiateTransferUseCase;
import com.example.temporal.transfer.domain.port.in.QueryTransferUseCase;
import com.example.temporal.transfer.config.FeatureFlagService;
import com.example.temporal.transfer.infrastructure.adapter.in.rest.dto.BatchCancelRequest;
import com.example.temporal.transfer.infrastructure.adapter.in.rest.mapper.TransferRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST adapter for transfer operations
 * Adapts HTTP requests to domain use cases
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferRestController {

    private static final String CONTROL_DISABLED_ERROR = "Control functionality is temporarily disabled";

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final QueryTransferUseCase queryTransferUseCase;
    private final ControlTransferUseCase controlTransferUseCase;
    private final FeatureFlagService featureFlagService;
    private final TransferRestMapper transferRestMapper;

    @PostMapping
    public ResponseEntity<TransferInitiationResponse> initiateTransfer(@RequestBody TransferRequest request) {
        log.info("REST: Initiating transfer: {}", request);

        // Convert DTO to domain command
        InitiateTransferUseCase.InitiateTransferCommand command =
                transferRestMapper.toInitiateTransferCommand(request);

        // Execute use case
        InitiateTransferUseCase.TransferInitiationResult result =
                initiateTransferUseCase.initiateTransfer(command);

        // Convert domain result to DTO
        TransferInitiationResponse response = transferRestMapper.toInitiationResponse(result);

        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{transferId}/status")
    public ResponseEntity<TransferResponse> getTransferStatus(@PathVariable final Long transferId) {
        log.debug("REST: Getting transfer status for ID: {}", transferId);

        final String workflowId = "transfer-" + transferId;
        final QueryTransferUseCase.TransferStatusResult result =
                queryTransferUseCase.getTransferStatus(workflowId);

        final TransferResponse response = transferRestMapper.toTransferResponse(result.getTransfer());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<TransferResponse> getTransferStatusByWorkflowId(
            @PathVariable final String workflowId) {
        log.debug("REST: Getting transfer status for workflow: {}", workflowId);

        final QueryTransferUseCase.TransferStatusResult result =
                queryTransferUseCase.getTransferStatus(workflowId);

        final TransferResponse response = transferRestMapper.toTransferResponse(result.getTransfer());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransferResponse>> getTransfersByAccount(
            @PathVariable final String accountNumber) {
        log.debug("REST: Getting transfers for account: {}", accountNumber);

        final List<TransferDomain> transfers = queryTransferUseCase.getTransfersByAccount(accountNumber);
        final List<TransferResponse> responses = transferRestMapper.toTransferResponses(transfers);

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/transfer/{transferId}")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable final Long transferId) {
        log.debug("REST: Getting transfer by ID: {}", transferId);

        final TransferDomain transfer = queryTransferUseCase.getTransferById(transferId)
                .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        return ResponseEntity.ok(transferRestMapper.toTransferResponse(transfer));
    }

    // ========== CONTROL ENDPOINTS ==========

    @PostMapping("/{workflowId}/pause")
    public ResponseEntity<?> pauseTransfer(@PathVariable final String workflowId) {
        log.info("REST: Request to pause transfer: {}", workflowId);

        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.simple(CONTROL_DISABLED_ERROR));
        }

        final ControlTransferUseCase.ControlResult result =
                controlTransferUseCase.pauseTransfer(workflowId);
        final TransferControlResponse response = transferRestMapper.toControlResponse(result);

        return result.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/{workflowId}/resume")
    public ResponseEntity<?> resumeTransfer(@PathVariable final String workflowId) {
        log.info("REST: Request to resume transfer: {}", workflowId);

        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.simple(CONTROL_DISABLED_ERROR));
        }

        final ControlTransferUseCase.ControlResult result =
                controlTransferUseCase.resumeTransfer(workflowId);
        final TransferControlResponse response = transferRestMapper.toControlResponse(result);

        return result.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<?> cancelTransfer(
            @PathVariable final String workflowId,
            @RequestBody @Valid final TransferControlRequest request) {

        log.info("REST: Request to cancel transfer: {} with reason: {}", workflowId, request.getReason());

        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse.simple(CONTROL_DISABLED_ERROR));
        }

        final String reason = request.getReason() != null
                ? request.getReason()
                : "Cancelled by user request";

        final ControlTransferUseCase.ControlResult result =
                controlTransferUseCase.cancelTransfer(workflowId, reason);
        final TransferControlResponse response = transferRestMapper.toControlResponse(result);

        return result.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/{workflowId}/control-status")
    public ResponseEntity<?> getControlStatus(@PathVariable final String workflowId) {
        log.debug("REST: Getting control status for: {}", workflowId);

        try {
            final ControlTransferUseCase.ControlStatusResult result =
                    controlTransferUseCase.getControlStatus(workflowId);

            return ResponseEntity.ok(result);

        } catch (final Exception e) {
            log.error("Error getting control status for {}: {}", workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.withDetails(
                            "Workflow not found",
                            Map.of("workflowId", workflowId)));
        }
    }

    // ========== BATCH OPERATIONS ==========

    @PostMapping("/batch/pause")
    public ResponseEntity<BatchOperationResponse> pauseMultipleTransfers(
            @RequestBody final List<String> workflowIds) {
        log.info("REST: Request to pause {} transfers", workflowIds.size());

        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(BatchOperationResponse.builder()
                            .total(workflowIds.size())
                            .successful(0)
                            .failed(workflowIds.size())
                            .results(List.of())
                            .build());
        }

        final List<BatchOperationResponse.BatchOperationResult> results = workflowIds.stream()
                .map(this::pauseSingleTransfer)
                .toList();

        final long successCount = results.stream()
                .filter(BatchOperationResponse.BatchOperationResult::isSuccess)
                .count();

        final BatchOperationResponse response = BatchOperationResponse.builder()
                .total(workflowIds.size())
                .successful(successCount)
                .failed(workflowIds.size() - successCount)
                .results(results)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch/cancel")
    public ResponseEntity<BatchOperationResponse> cancelMultipleTransfers(
            @RequestBody final BatchCancelRequest request) {

        final List<String> workflowIds = request.getWorkflowIds();
        final String reason = request.getReason() != null
                ? request.getReason()
                : "Batch cancellation";

        log.info("REST: Request to cancel {} transfers", workflowIds.size());

        if (!featureFlagService.isControlEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(BatchOperationResponse.builder()
                            .total(workflowIds.size())
                            .successful(0)
                            .failed(workflowIds.size())
                            .reason(reason)
                            .results(List.of())
                            .build());
        }

        final List<BatchOperationResponse.BatchOperationResult> results = workflowIds.stream()
                .map(workflowId -> cancelSingleTransfer(workflowId, reason))
                .toList();

        final long successCount = results.stream()
                .filter(BatchOperationResponse.BatchOperationResult::isSuccess)
                .count();

        final BatchOperationResponse response = BatchOperationResponse.builder()
                .total(workflowIds.size())
                .successful(successCount)
                .failed(workflowIds.size() - successCount)
                .reason(reason)
                .results(results)
                .build();

        return ResponseEntity.ok(response);
    }

    // ========== HELPER METHODS ==========

    /**
     * Pauses a single transfer and returns the result.
     */
    private BatchOperationResponse.BatchOperationResult pauseSingleTransfer(final String workflowId) {
        try {
            final ControlTransferUseCase.ControlResult result =
                    controlTransferUseCase.pauseTransfer(workflowId);

            return BatchOperationResponse.BatchOperationResult.builder()
                    .workflowId(workflowId)
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .build();

        } catch (final Exception e) {
            return BatchOperationResponse.BatchOperationResult.builder()
                    .workflowId(workflowId)
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    /**
     * Cancels a single transfer and returns the result.
     */
    private BatchOperationResponse.BatchOperationResult cancelSingleTransfer(
            final String workflowId,
            final String reason) {
        try {
            final ControlTransferUseCase.ControlResult result =
                    controlTransferUseCase.cancelTransfer(workflowId, reason);

            return BatchOperationResponse.BatchOperationResult.builder()
                    .workflowId(workflowId)
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .build();

        } catch (final Exception e) {
            return BatchOperationResponse.BatchOperationResult.builder()
                    .workflowId(workflowId)
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }
}
