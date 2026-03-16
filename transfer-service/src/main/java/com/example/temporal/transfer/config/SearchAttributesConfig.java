package com.example.temporal.transfer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Temporal Search Attributes
 * Defines custom search attributes for transfer workflows
 * 
 * Note: Search attributes must be pre-configured in Temporal cluster.
 * Run this to configure (requires temporal admin CLI):
 * 
 * temporal operator search-attribute create --namespace default \
 *   --name TransferAmount --type double
 * temporal operator search-attribute create --namespace default \
 *   --name SourceAccount --type keyword
 * temporal operator search-attribute create --namespace default \
 *   --name DestinationAccount --type keyword
 * temporal operator search-attribute create --namespace default \
 *   --name Currency --type keyword
 * temporal operator search-attribute create --namespace default \
 *   --name TransferStatus --type keyword
 * temporal operator search-attribute create --namespace default \
 *   --name Priority --type int
 */
@Slf4j
@Component
public class SearchAttributesConfig {

    /**
     * Creates search attributes map for a transfer
     * This map can be passed to Workflow.upsertSearchAttributes()
     */
    public Map<String, Object> createSearchAttributes(
            BigDecimal amount,
            String sourceAccount,
            String destinationAccount,
            String currency,
            String status,
            Integer priority) {
        
        Map<String, Object> attributes = new HashMap<>();
        
        if (amount != null) {
            attributes.put("TransferAmount", amount.doubleValue());
        }
        if (sourceAccount != null) {
            attributes.put("SourceAccount", sourceAccount);
        }
        if (destinationAccount != null) {
            attributes.put("DestinationAccount", destinationAccount);
        }
        if (currency != null) {
            attributes.put("Currency", currency);
        }
        if (status != null) {
            attributes.put("TransferStatus", status);
        }
        if (priority != null) {
            attributes.put("Priority", priority);
        }

        log.debug("Created search attributes: {}", attributes);
        return attributes;
    }

    /**
     * Calculates priority based on transfer amount
     * Higher amounts = higher priority
     */
    public int calculatePriority(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }
        
        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return 5; // Highest priority
        } else if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            return 4;
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return 3;
        } else if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            return 2;
        } else if (amount.compareTo(new BigDecimal("100")) >= 0) {
            return 1;
        } else {
            return 0; // Normal priority
        }
    }

    /**
     * Builds a Temporal visibility query string
     */
    public String buildQuery(Map<String, Object> filters) {
        StringBuilder query = new StringBuilder("WorkflowType='MoneyTransferWorkflow'");
        
        if (filters.containsKey("TransferAmount")) {
            query.append(" AND TransferAmount = ").append(filters.get("TransferAmount"));
        }
        
        if (filters.containsKey("SourceAccount")) {
            query.append(" AND SourceAccount = '").append(filters.get("SourceAccount")).append("'");
        }
        
        if (filters.containsKey("DestinationAccount")) {
            query.append(" AND DestinationAccount = '").append(filters.get("DestinationAccount")).append("'");
        }
        
        if (filters.containsKey("Currency")) {
            query.append(" AND Currency = '").append(filters.get("Currency")).append("'");
        }
        
        if (filters.containsKey("TransferStatus")) {
            query.append(" AND TransferStatus = '").append(filters.get("TransferStatus")).append("'");
        }
        
        if (filters.containsKey("Priority")) {
            query.append(" AND Priority = ").append(filters.get("Priority"));
        }
        
        log.info("Built Temporal search query: {}", query);
        return query.toString();
    }
}
