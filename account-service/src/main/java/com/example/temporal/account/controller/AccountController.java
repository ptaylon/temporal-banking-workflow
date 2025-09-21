package com.example.temporal.account.controller;

import com.example.temporal.account.service.AccountService;
import com.example.temporal.common.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody final Account account) {
        return ResponseEntity.ok(accountService.createAccount(account));
    }

    @GetMapping
    public ResponseEntity<List<Account>> getAccounts(@RequestParam final List<String> accountNumbers) {
        return ResponseEntity.ok(accountService.getAccounts(accountNumbers));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable final String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<Void> creditAccount(@PathVariable final String accountNumber, @RequestParam("amount") final BigDecimal amount) {
        accountService.creditAccount(accountNumber, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<Void> debitAccount(@PathVariable final String accountNumber, @RequestParam("amount") final BigDecimal amount) {
        accountService.debitAccount(accountNumber, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lock")
    public ResponseEntity<Void> debitAccount(@RequestParam("sourceAccount") final String sourceAccount, @RequestParam("destinationAccount") String destinationAccount) {
        accountService.lockAccounts(sourceAccount, destinationAccount);
        return ResponseEntity.ok().build();
    }

}