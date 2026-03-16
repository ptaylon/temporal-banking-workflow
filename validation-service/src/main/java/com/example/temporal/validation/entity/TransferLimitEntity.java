package com.example.temporal.validation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * JPA entity for transfer limits
 */
@Data
@Entity
@Accessors(chain = true)
@Table(name = "transfer_limits")
public class TransferLimitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private BigDecimal singleTransferLimit;

    @Column(nullable = false)
    private BigDecimal dailyTransferLimit;

    @Column(nullable = false)
    private BigDecimal monthlyTransferLimit;

    private String currency;
}
