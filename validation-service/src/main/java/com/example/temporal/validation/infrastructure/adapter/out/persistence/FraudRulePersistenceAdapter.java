package com.example.temporal.validation.infrastructure.adapter.out.persistence;

import com.example.temporal.validation.domain.port.out.FraudRulePort;
import com.example.temporal.validation.entity.FraudDetectionRuleEntity;
import com.example.temporal.validation.repository.FraudDetectionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter for fraud rule persistence
 * Implements the FraudRulePort using JPA repository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudRulePersistenceAdapter implements FraudRulePort {

    private final FraudDetectionRuleRepository fraudDetectionRuleRepository;

    @Override
    public List<FraudRuleInfo> getActiveRules() {
        try {
            List<FraudDetectionRuleEntity> rules = fraudDetectionRuleRepository.findByIsActiveTrue();

            return rules.stream()
                    .map(this::toFraudRuleInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching active fraud rules: {}", e.getMessage());
            return List.of();
        }
    }

    private FraudRuleInfo toFraudRuleInfo(FraudDetectionRuleEntity rule) {
        return new FraudRuleInfo(
                rule.getId(),
                rule.getRuleName(),
                rule.getRuleType(),
                rule.getThreshold(),
                rule.getTimeWindowMinutes(),
                rule.getIsActive(),
                rule.getDescription()
        );
    }
}
