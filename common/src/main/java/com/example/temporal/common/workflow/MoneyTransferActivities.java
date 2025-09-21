package com.example.temporal.common.workflow;

import com.example.temporal.common.dto.TransferRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;

@ActivityInterface
public interface MoneyTransferActivities {

    @ActivityMethod
    void validateTransfer(final TransferRequest request);

    @ActivityMethod
    void lockAccounts(final String sourceAccountNumber, final String destinationAccountNumber);

    @ActivityMethod
    void debitAccount(final String accountNumber, final BigDecimal amount);

    @ActivityMethod
    void creditAccount(final String accountNumber, final BigDecimal amount);

    @ActivityMethod
    void unlockAccounts(final String sourceAccountNumber, final String destinationAccountNumber);

    @ActivityMethod
    void compensateDebit(final String accountNumber, final BigDecimal amount);

    @ActivityMethod
    void compensateCredit(final String accountNumber, final BigDecimal amount);

    @ActivityMethod
    void notifyTransferInitiated(final Long transferId);

    @ActivityMethod
    void notifyTransferCompleted(final Long transferId);

    @ActivityMethod
    void notifyTransferFailed(final Long transferId, final String reason);

}