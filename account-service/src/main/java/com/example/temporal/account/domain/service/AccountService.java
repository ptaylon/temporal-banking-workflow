package com.example.temporal.account.domain.service;

import com.example.temporal.account.domain.model.AccountDomain;
import com.example.temporal.account.domain.port.in.AccountOperationsUseCase;
import com.example.temporal.account.domain.port.in.CreateAccountUseCase;
import com.example.temporal.account.domain.port.in.QueryAccountUseCase;
import com.example.temporal.account.domain.port.out.AccountPersistencePort;
import com.example.temporal.common.message.ErrorMessages;
import com.example.temporal.common.message.MessageResolver;
import com.example.temporal.common.message.SuccessMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service implementing account use cases
 * Contains pure business logic without framework dependencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements CreateAccountUseCase, QueryAccountUseCase, AccountOperationsUseCase {

    private final AccountPersistencePort persistencePort;

    @Override
    @Transactional
    public CreateAccountResult createAccount(final CreateAccountCommand command) {
        log.info("Creating account: {}", command.getAccountNumber());

        try {
            validateCreateAccountCommand(command);

            final String idempotencyKey = getOrGenerateIdempotencyKey(command.getIdempotencyKey());

            return checkIdempotencyAndCreate(idempotencyKey, command);

        } catch (final IllegalArgumentException e) {
            log.error("Validation error creating account: {}", e.getMessage());
            return CreateAccountResult.error(
                    MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, e.getMessage()));
        } catch (final Exception e) {
            log.error("Error creating account: {}", e.getMessage(), e);
            return CreateAccountResult.error(
                    MessageResolver.resolveError(ErrorMessages.OPERATION_FAILED, e.getMessage()));
        }
    }

    /**
     * Validates the create account command.
     *
     * @param command the command to validate
     * @throws IllegalArgumentException if command is invalid
     */
    private void validateCreateAccountCommand(final CreateAccountCommand command) {
        if (command == null) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.VALIDATION_ERROR, "Command cannot be null"));
        }
        command.validate();
    }

    /**
     * Gets or generates an idempotency key.
     *
     * @param providedKey the provided key, or null to generate
     * @return the idempotency key
     */
    private String getOrGenerateIdempotencyKey(final String providedKey) {
        return providedKey != null ? providedKey : UUID.randomUUID().toString();
    }

    /**
     * Checks for existing account by idempotency key and creates if not exists.
     *
     * @param idempotencyKey the idempotency key
     * @param command the create command
     * @return the creation result
     */
    private CreateAccountResult checkIdempotencyAndCreate(
            final String idempotencyKey,
            final CreateAccountCommand command) {

        return persistencePort.findByIdempotencyKey(idempotencyKey)
                .map(existing -> handleExistingAccount(idempotencyKey, existing))
                .orElseGet(() -> createNewAccount(command, idempotencyKey));
    }

    /**
     * Handles the case when an account already exists for the idempotency key.
     *
     * @param idempotencyKey the idempotency key
     * @param existing the existing account
     * @return success result with existing account
     */
    private CreateAccountResult handleExistingAccount(
            final String idempotencyKey,
            final AccountDomain existing) {
        log.info("Account already exists for idempotency key: {}", idempotencyKey);
        return CreateAccountResult.success(existing);
    }

    /**
     * Creates a new account.
     *
     * @param command the create command
     * @param idempotencyKey the idempotency key
     * @return the creation result
     */
    private CreateAccountResult createNewAccount(
            final CreateAccountCommand command,
            final String idempotencyKey) {

        checkAccountNumberNotExists(command.getAccountNumber());

        final AccountDomain account = AccountDomain.create(
                command.getAccountNumber(),
                command.getOwnerName(),
                command.getInitialBalance(),
                command.getCurrency(),
                idempotencyKey
        );

        final AccountDomain savedAccount = persistencePort.save(account);

        log.info("Account created successfully: {}", savedAccount.getAccountNumber());
        return CreateAccountResult.success(savedAccount);
    }

    /**
     * Checks if account number already exists.
     *
     * @param accountNumber the account number to check
     * @throws IllegalArgumentException if account exists
     */
    private void checkAccountNumberNotExists(final String accountNumber) {
        if (persistencePort.existsByAccountNumber(accountNumber)) {
            throw new IllegalArgumentException(
                    MessageResolver.resolveError(ErrorMessages.DUPLICATE_ENTRY, accountNumber));
        }
    }

    @Override
    public Optional<AccountDomain> getAccountByNumber(final String accountNumber) {
        log.debug("Getting account by number: {}", accountNumber);
        return persistencePort.findByAccountNumber(accountNumber);
    }

    @Override
    public Optional<AccountDomain> getAccountById(final Long id) {
        log.debug("Getting account by ID: {}", id);
        return persistencePort.findById(id);
    }

    @Override
    public List<AccountDomain> getAccounts(final List<String> accountNumbers) {
        log.debug("Getting accounts: {}", accountNumbers);
        return persistencePort.findByAccountNumberIn(accountNumbers);
    }

    @Override
    public boolean accountExists(final String accountNumber) {
        return persistencePort.existsByAccountNumber(accountNumber);
    }

    @Override
    @Transactional
    public void lockAccounts(
            final String sourceAccountNumber,
            final String destinationAccountNumber) {
        log.info("Locking accounts: {} and {}", sourceAccountNumber, destinationAccountNumber);

        final AccountDomain source = findAccountWithLock(
                sourceAccountNumber,
                ErrorMessages.ENTITY_NOT_FOUND);
        final AccountDomain destination = findAccountWithLock(
                destinationAccountNumber,
                ErrorMessages.ENTITY_NOT_FOUND);

        log.debug("Accounts locked: {} and {}", source.getAccountNumber(), destination.getAccountNumber());
    }

    @Override
    @Transactional
    public void debitAccount(final String accountNumber, final BigDecimal amount) {
        log.info("Debiting account {} amount {}", accountNumber, amount);

        final AccountDomain account = findAccountWithLock(
                accountNumber,
                ErrorMessages.ENTITY_NOT_FOUND);

        final AccountDomain updatedAccount = account.debit(amount);

        persistencePort.update(updatedAccount);

        log.info("Account debited successfully: {} new balance: {}",
                accountNumber, updatedAccount.getBalance());
    }

    @Override
    @Transactional
    public void creditAccount(final String accountNumber, final BigDecimal amount) {
        log.info("Crediting account {} amount {}", accountNumber, amount);

        final AccountDomain account = findAccountWithLock(
                accountNumber,
                ErrorMessages.ENTITY_NOT_FOUND);

        final AccountDomain updatedAccount = account.credit(amount);

        persistencePort.update(updatedAccount);

        log.info("Account credited successfully: {} new balance: {}",
                accountNumber, updatedAccount.getBalance());
    }

    @Override
    public BigDecimal getBalance(final String accountNumber) {
        log.debug("Getting balance for account: {}", accountNumber);

        final AccountDomain account = findAccount(
                accountNumber,
                ErrorMessages.ENTITY_NOT_FOUND);

        return account.getBalance();
    }

    /**
     * Finds an account with pessimistic lock.
     *
     * @param accountNumber the account number
     * @param errorKey the error message key
     * @return the found account
     * @throws IllegalArgumentException if account not found
     */
    private AccountDomain findAccountWithLock(
            final String accountNumber,
            final String errorKey) {
        return persistencePort.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageResolver.resolveError(errorKey, accountNumber)));
    }

    /**
     * Finds an account without lock.
     *
     * @param accountNumber the account number
     * @param errorKey the error message key
     * @return the found account
     * @throws IllegalArgumentException if account not found
     */
    private AccountDomain findAccount(final String accountNumber, final String errorKey) {
        return persistencePort.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageResolver.resolveError(errorKey, accountNumber)));
    }
}
