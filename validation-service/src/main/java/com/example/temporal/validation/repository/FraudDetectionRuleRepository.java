package com.example.temporal.validation.repository;

import com.example.temporal.validation.model.FraudDetectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudDetectionRuleRepository extends JpaRepository<FraudDetectionRule, Long> {
    List<FraudDetectionRule> findByIsActiveTrue();
    List<FraudDetectionRule> findByRuleTypeAndIsActiveTrue(final String ruleType);
}