package com.example.temporal.validation.infrastructure.adapter.out.persistence;

import com.example.temporal.validation.domain.port.out.TransferLimitPort;
import com.example.temporal.validation.entity.TransferLimitEntity;
import com.example.temporal.validation.repository.TransferLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter for transfer limit persistence
 * Implements the TransferLimitPort using JPA repository
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferLimitPersistenceAdapter implements TransferLimitPort {

    private final TransferLimitRepository transferLimitRepository;

    @Override
    public Optional<TransferLimitInfo> getByAccountTypeAndCurrency(String accountType, String currency) {
        try {
            return transferLimitRepository
                    .findByAccountTypeAndCurrency(accountType, currency)
                    .map(this::toTransferLimitInfo);
        } catch (Exception e) {
            log.error("Error fetching transfer limit for {}/{}: {}",
                     accountType, currency, e.getMessage());
            return Optional.empty();
        }
    }

    private TransferLimitInfo toTransferLimitInfo(TransferLimitEntity limit) {
        return new TransferLimitInfo(
                limit.getId(),
                limit.getAccountType(),
                limit.getSingleTransferLimit(),
                limit.getDailyTransferLimit(),
                limit.getMonthlyTransferLimit(),
                limit.getCurrency()
        );
    }
}
