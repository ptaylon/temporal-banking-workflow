package com.example.temporal.transfer.repository;

import com.example.temporal.common.model.Transfer;
import com.example.temporal.common.model.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    
    List<Transfer> findBySourceAccountNumberOrDestinationAccountNumber(
            String sourceAccountNumber, String destinationAccountNumber);
    
    List<Transfer> findByStatus(TransferStatus status);
    
    @Query("SELECT t FROM Transfer t WHERE t.sourceAccountNumber = :accountNumber OR t.destinationAccountNumber = :accountNumber")
    List<Transfer> findByAccountNumber(@Param("accountNumber") String accountNumber);
    
    Optional<Transfer> findByIdAndStatus(Long id, TransferStatus status);
}