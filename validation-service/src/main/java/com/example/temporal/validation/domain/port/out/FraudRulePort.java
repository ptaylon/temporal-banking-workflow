package com.example.temporal.validation.domain.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Port for fraud rule operations
 * Defines what the domain needs for fraud detection
 */
public interface FraudRulePort {

    /**
     * Gets all active fraud rules
     * @return list of active fraud rules
     */
    List<FraudRuleInfo> getActiveRules();

    /**
     * Fraud rule information DTO
     */
    record FraudRuleInfo(
            Long id,
            String ruleName,
            String ruleType,
            BigDecimal threshold,
            Integer timeWindowMinutes,
            boolean active,
            String description
    ) {}
}
