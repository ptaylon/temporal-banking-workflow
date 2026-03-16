package com.example.temporal.validation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * JPA entity for fraud detection rules
 */
@Data
@Entity
@Accessors(chain = true)
@Table(name = "fraud_detection_rules")
public class FraudDetectionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String ruleType;

    @Column(nullable = false)
    private BigDecimal threshold;

    @Column(nullable = false)
    private Integer timeWindowMinutes;

    @Column(nullable = false)
    private Boolean isActive;

    private String description;
}
