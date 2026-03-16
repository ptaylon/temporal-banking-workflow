package com.example.temporal.transfer.infrastructure.adapter.in.rest;

import com.example.temporal.transfer.service.TransferSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST adapter for transfer search operations
 * Exposes search capabilities using Temporal search attributes
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers/search")
@RequiredArgsConstructor
public class TransferSearchRestController {

    private final TransferSearchService transferSearchService;

    /**
     * Advanced search for transfers
     */
    @GetMapping
    public ResponseEntity<List<TransferSearchService.TransferSearchResult>> searchTransfers(
            @RequestParam(required = false) String sourceAccount,
            @RequestParam(required = false) String destinationAccount,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) Integer minPriority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {

        log.info("REST: Advanced search for transfers");

        TransferSearchService.SearchCriteria criteria = TransferSearchService.SearchCriteria.builder()
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .status(status)
                .currency(currency)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .minPriority(minPriority)
                .startTime(startTime)
                .build();

        List<TransferSearchService.TransferSearchResult> results =
                transferSearchService.searchTransfers(criteria);

        return ResponseEntity.ok(results);
    }

    /**
     * Search transfers by account number
     */
    @GetMapping("/by-account/{accountNumber}")
    public ResponseEntity<List<TransferSearchService.TransferSearchResult>> getTransfersByAccount(
            @PathVariable String accountNumber) {

        log.info("REST: Search transfers by account: {}", accountNumber);

        List<TransferSearchService.TransferSearchResult> results =
                transferSearchService.getTransfersByAccount(accountNumber);

        return ResponseEntity.ok(results);
    }

    /**
     * Search transfers by amount range
     */
    @GetMapping("/by-amount-range")
    public ResponseEntity<List<TransferSearchService.TransferSearchResult>> getTransfersByAmountRange(
            @RequestParam Double minAmount,
            @RequestParam Double maxAmount) {

        log.info("REST: Search transfers by amount range: {} - {}", minAmount, maxAmount);

        List<TransferSearchService.TransferSearchResult> results =
                transferSearchService.getTransfersByAmountRange(minAmount, maxAmount);

        return ResponseEntity.ok(results);
    }

    /**
     * Search transfers by status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<TransferSearchService.TransferSearchResult>> getTransfersByStatus(
            @PathVariable String status) {

        log.info("REST: Search transfers by status: {}", status);

        List<TransferSearchService.TransferSearchResult> results =
                transferSearchService.getTransfersByStatus(status);

        return ResponseEntity.ok(results);
    }

    /**
     * Get high priority transfers
     */
    @GetMapping("/high-priority")
    public ResponseEntity<List<TransferSearchService.TransferSearchResult>> getHighPriorityTransfers() {

        log.info("REST: Get high priority transfers");

        List<TransferSearchService.TransferSearchResult> results =
                transferSearchService.getHighPriorityTransfers();

        return ResponseEntity.ok(results);
    }

    /**
     * Get transfer summary statistics
     */
    @GetMapping("/analytics/summary")
    public ResponseEntity<TransferSearchService.TransferSummary> getTransferSummary() {

        log.info("REST: Get transfer summary statistics");

        TransferSearchService.TransferSummary summary =
                transferSearchService.getTransferSummary();

        return ResponseEntity.ok(summary);
    }

    /**
     * Get transfer analytics by currency
     */
    @GetMapping("/analytics/by-currency")
    public ResponseEntity<Map<String, TransferSearchService.TransferSummary>> getAnalyticsByCurrency() {

        log.info("REST: Get analytics by currency");

        // This would require additional implementation to group by currency
        // For now, return a simple response
        return ResponseEntity.ok(Map.of(
                "BRL", transferSearchService.getTransferSummary()
        ));
    }
}
