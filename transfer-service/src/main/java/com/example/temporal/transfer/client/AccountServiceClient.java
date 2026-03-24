package com.example.temporal.transfer.client;

import com.example.temporal.transfer.infrastructure.adapter.out.http.LockAccountsRequest;
import com.example.temporal.transfer.infrastructure.adapter.out.http.OperationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service", url = "${service.account.url}")
public interface AccountServiceClient {

    @PostMapping("/api/accounts/lock")
    void lockAccounts(@RequestBody LockAccountsRequest request);

    @PostMapping("/api/accounts/{accountNumber}/debit")
    void debitAccount(
            @PathVariable final String accountNumber,
            @RequestBody OperationRequest request
    );

    @PostMapping("/api/accounts/{accountNumber}/credit")
    void creditAccount(
            @PathVariable final String accountNumber,
            @RequestBody OperationRequest request
    );

}