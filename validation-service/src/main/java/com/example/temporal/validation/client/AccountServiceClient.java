package com.example.temporal.validation.client;

import com.example.temporal.common.model.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

/**
 * Feign client for account service with error handling
 */
@FeignClient(
    name = "account-service", 
    url = "${service.account.url:http://localhost:8081}",
    fallback = AccountServiceClientFallback.class
)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{accountNumber}")
    Account getAccount(@PathVariable final String accountNumber);
}
