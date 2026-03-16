package com.example.temporal.validation.domain.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port for transfer limit operations
 * Defines what the domain needs for transfer limit checks
 */
public interface TransferLimitPort {

    /**
     * Gets transfer limit by account type and currency
     * @param accountType the account type
     * @param currency the currency
     * @return the transfer limit if configured
     */
    Optional<TransferLimitInfo> getByAccountTypeAndCurrency(String accountType, String currency);

    /**
     * Transfer limit information DTO
     */
    record TransferLimitInfo(
            Long id,
            String accountType,
            BigDecimal singleTransferLimit,
            BigDecimal dailyTransferLimit,
            BigDecimal monthlyTransferLimit,
            String currency
    ) {}
}
