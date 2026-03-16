package com.example.temporal.validation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for transfer validation
 */
@Data
@Entity
@Accessors(chain = true)
@Table(name = "transfer_validations")
public class TransferValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id")
    private String transferId;

    @Column(name = "source_account_number", nullable = false)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false)
    private String destinationAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "validation_result", nullable = false)
    @Enumerated(EnumType.STRING)
    private ValidationResult validationResult;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "fraud_score")
    private Integer fraudScore;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    /**
     * Validation result enum
     */
    public enum ValidationResult {
        APPROVED,
        REJECTED,
        PENDING
    }
}
