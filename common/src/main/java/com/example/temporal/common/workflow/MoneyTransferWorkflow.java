package com.example.temporal.common.workflow;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.common.dto.TransferControlStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MoneyTransferWorkflow {

    String QUEUE_NAME = "MONEY_TRANSFER_WORKFLOW_QUEUE";

    @WorkflowMethod
    TransferResponse executeTransfer(TransferRequest transferRequest);

    @QueryMethod
    TransferResponse getStatus();

    // Signal Methods para controle de transferência
    @SignalMethod
    void pauseTransfer();

    @SignalMethod
    void resumeTransfer();

    @SignalMethod
    void cancelTransfer(String reason);

    // Query Methods para status de controle
    @QueryMethod
    boolean isPaused();

    @QueryMethod
    TransferControlStatus getControlStatus();

}