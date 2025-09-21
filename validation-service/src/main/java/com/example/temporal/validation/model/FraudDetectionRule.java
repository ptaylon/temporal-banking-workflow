package com.example.temporal.validation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Entity
@Accessors(chain = true)
@Table(name = "fraud_detection_rules")
public class FraudDetectionRule {
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