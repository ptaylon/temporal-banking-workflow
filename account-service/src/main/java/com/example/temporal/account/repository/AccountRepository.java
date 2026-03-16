package com.example.temporal.account.repository;

import com.example.temporal.common.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    List<Account> findByAccountNumberIn(List<String> accountNumbers);

    /**
     * Find account by idempotency key (for idempotent operations)
     */
    Optional<Account> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if account exists with account number
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Check if account exists with idempotency key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);
}