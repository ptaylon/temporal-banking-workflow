package com.example.temporal.validation.repository;

import com.example.temporal.validation.entity.FraudDetectionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudDetectionRuleRepository extends JpaRepository<FraudDetectionRuleEntity, Long> {
    List<FraudDetectionRuleEntity> findByIsActiveTrue();
    List<FraudDetectionRuleEntity> findByRuleTypeAndIsActiveTrue(final String ruleType);
}