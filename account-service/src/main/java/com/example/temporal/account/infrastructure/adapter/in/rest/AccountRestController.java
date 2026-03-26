package com.example.temporal.account.infrastructure.adapter.in.rest;

import com.example.temporal.account.domain.model.AccountDomain;
import com.example.temporal.account.domain.port.in.CreateAccountUseCase;
import com.example.temporal.account.domain.port.in.QueryAccountUseCase;
import com.example.temporal.account.domain.service.AccountOperationService;
import com.example.temporal.common.aspect.IdempotentAspect.IdempotentOperationException;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.AccountCreateRequest;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.AccountResponse;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.BalanceResponse;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.LockAccountsRequest;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.MessageResponse;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.OperationRequest;
import com.example.temporal.account.infrastructure.adapter.in.rest.mapper.AccountRestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST adapter for account operations
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final CreateAccountUseCase createAccountUseCase;
    private final QueryAccountUseCase queryAccountUseCase;
    private final AccountOperationService accountOperationService;
    private final AccountRestMapper accountRestMapper;

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody final AccountCreateRequest request) {
        log.info("REST: Creating account: {}", request.getAccountNumber());

        final CreateAccountUseCase.CreateAccountCommand command =
                accountRestMapper.toCreateAccountCommand(request);

        final CreateAccountUseCase.CreateAccountResult result =
                createAccountUseCase.createAccount(command);

        if ("ERROR".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable final String accountNumber) {
        log.debug("REST: Getting account: {}", accountNumber);

        final AccountDomain account = queryAccountUseCase.getAccountByNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        return ResponseEntity.ok(accountRestMapper.toAccountResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @RequestParam final List<String> accountNumbers) {
        log.debug("REST: Getting accounts: {}", accountNumbers);

        final List<AccountDomain> accounts = queryAccountUseCase.getAccounts(accountNumbers);
        final List<AccountResponse> responses = accountRestMapper.toAccountResponses(accounts);

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/lock")
    public ResponseEntity<MessageResponse> lockAccounts(
            @RequestBody final LockAccountsRequest request) {
        log.info("REST: Locking accounts: {} and {}",
                request.getSourceAccountNumber(), request.getDestinationAccountNumber());

        accountOperationService.lockAccounts(
                request.getSourceAccountNumber(),
                request.getDestinationAccountNumber());

        final MessageResponse response = new MessageResponse();
        response.setMessage("Accounts locked successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<MessageResponse> debitAccount(
            @PathVariable final String accountNumber,
            @RequestBody final OperationRequest request) {

        log.info("REST: Debiting account: {} amount: {}", accountNumber, request.getAmount());

        try {
            accountOperationService.debitWithIdempotency(
                    accountNumber,
                    request.getAmount(),
                    request.getIdempotencyKey()
            );

            MessageResponse response = new MessageResponse();
            response.setMessage("Account debited successfully");
            return ResponseEntity.ok(response);

        } catch (IdempotentOperationException e) {
            MessageResponse response = new MessageResponse();
            response.setMessage("Operation already processed");
            return ResponseEntity.ok()
                    .header("X-Idempotency-Status", "ALREADY_PROCESSED")
                    .body(response);

        } catch (Exception e) {
            log.error("Error debiting account: {}", e.getMessage());
            MessageResponse response = new MessageResponse();
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<MessageResponse> creditAccount(
            @PathVariable final String accountNumber,
            @RequestBody final OperationRequest request) {

        log.info("REST: Crediting account: {} amount: {}", accountNumber, request.getAmount());

        try {
            accountOperationService.creditWithIdempotency(
                    accountNumber,
                    request.getAmount(),
                    request.getIdempotencyKey()
            );

            MessageResponse response = new MessageResponse();
            response.setMessage("Account credited successfully");
            return ResponseEntity.ok(response);

        } catch (IdempotentOperationException e) {
            MessageResponse response = new MessageResponse();
            response.setMessage("Operation already processed");
            return ResponseEntity.ok()
                    .header("X-Idempotency-Status", "ALREADY_PROCESSED")
                    .body(response);

        } catch (Exception e) {
            log.error("Error crediting account: {}", e.getMessage());
            MessageResponse response = new MessageResponse();
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable final String accountNumber) {
        log.debug("REST: Getting balance for account: {}", accountNumber);

        final BigDecimal balance = accountOperationService.getBalance(accountNumber);

        final BalanceResponse response = new BalanceResponse();
        response.setAccountNumber(accountNumber);
        response.setBalance(balance);
        return ResponseEntity.ok(response);
    }
}
