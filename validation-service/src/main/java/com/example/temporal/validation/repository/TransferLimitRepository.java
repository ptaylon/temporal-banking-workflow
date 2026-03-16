package com.example.temporal.validation.repository;

import com.example.temporal.validation.entity.TransferLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferLimitRepository extends JpaRepository<TransferLimitEntity, Long> {
    Optional<TransferLimitEntity> findByAccountTypeAndCurrency(String accountType, String currency);
}