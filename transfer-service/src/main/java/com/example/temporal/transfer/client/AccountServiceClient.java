package com.example.temporal.transfer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${service.account.url}")
public interface AccountServiceClient {
    
    @PostMapping("/api/accounts/lock")
    void lockAccounts(
            @RequestParam("sourceAccount") final String sourceAccount,
            @RequestParam("destinationAccount") final String destinationAccount
    );

    @PostMapping("/api/accounts/{accountNumber}/debit")
    void debitAccount(
            @PathVariable final String accountNumber,
            @RequestParam("amount") final BigDecimal amount
    );

    @PostMapping("/api/accounts/{accountNumber}/credit")
    void creditAccount(
            @PathVariable final String accountNumber,
            @RequestParam("amount") final BigDecimal amount
    );

}