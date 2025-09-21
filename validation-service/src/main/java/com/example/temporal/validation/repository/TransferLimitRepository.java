package com.example.temporal.validation.repository;

import com.example.temporal.validation.model.TransferLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferLimitRepository extends JpaRepository<TransferLimit, Long> {
    Optional<TransferLimit> findByAccountTypeAndCurrency(String accountType, String currency);
}