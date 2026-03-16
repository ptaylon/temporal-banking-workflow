package com.example.temporal.account.service;

import com.example.temporal.account.repository.AccountRepository;
import com.example.temporal.common.model.Account;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public Account getAccount(final String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional
    public Account createAccount(final Account account) {
        return accountRepository.save(account);
    }

    @Transactional
    public void lockAccounts(
            final String sourceAccountNumber,
            final String destinationAccountNumber) {
        accountRepository.findByAccountNumberWithLock(sourceAccountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Source account not found: " + sourceAccountNumber));
        accountRepository.findByAccountNumberWithLock(destinationAccountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Destination account not found: " + destinationAccountNumber));
    }

    @Transactional
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountNumber));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in account: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    @Transactional
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountNumber));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
    }

    @Transactional
    public List<Account> getAccounts(final List<String> accountNumbers) {
        return accountRepository.findByAccountNumberIn(accountNumbers);
    }
}