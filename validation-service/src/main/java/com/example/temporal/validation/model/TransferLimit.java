package com.example.temporal.validation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Entity
@Accessors(chain = true)
@Table(name = "transfer_limits")
public class TransferLimit {
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