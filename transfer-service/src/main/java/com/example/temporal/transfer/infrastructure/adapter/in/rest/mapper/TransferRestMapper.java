package com.example.temporal.transfer.infrastructure.adapter.in.rest.mapper;

import com.example.temporal.common.dto.TransferInitiationResponse;
import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.common.dto.TransferResponse;
import com.example.temporal.transfer.domain.model.TransferDomain;
import com.example.temporal.transfer.domain.port.in.ControlTransferUseCase;
import com.example.temporal.transfer.domain.port.in.InitiateTransferUseCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper for Transfer REST API - converts between DTOs and domain objects
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransferRestMapper {

    TransferRestMapper INSTANCE = Mappers.getMapper(TransferRestMapper.class);

    // ========== TransferRequest ↔ Domain Command ==========

    @Mapping(target = "delayInSeconds", ignore = true)
    @Mapping(target = "timeoutInSeconds", ignore = true)
    @Mapping(target = "allowCancelDuringDelay", ignore = true)
    InitiateTransferUseCase.InitiateTransferCommand toInitiateTransferCommand(TransferRequest request);

    // ========== TransferDomain ↔ TransferResponse ==========

    default TransferResponse toTransferResponse(TransferDomain domain) {
        if (domain == null) {
            return null;
        }
        return new TransferResponse()
                .setTransferId(domain.getId())
                .setSourceAccountNumber(domain.getSourceAccountNumber())
                .setDestinationAccountNumber(domain.getDestinationAccountNumber())
                .setAmount(domain.getAmount())
                .setCurrency(domain.getCurrency())
                .setStatus(domain.getStatus())
                .setFailureReason(domain.getFailureReason())
                .setCreatedAt(domain.getCreatedAt())
                .setUpdatedAt(domain.getUpdatedAt());
    }

    default List<TransferResponse> toTransferResponses(List<TransferDomain> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toTransferResponse)
                .toList();
    }

    // ========== TransferInitiationResult ↔ TransferInitiationResponse ==========

    @Mapping(target = "initiatedAt", expression = "java(LocalDateTime.now())")
    TransferInitiationResponse toInitiationResponse(InitiateTransferUseCase.TransferInitiationResult result);

    // ========== ControlResult ↔ TransferControlResponse ==========

    @Mapping(target = "workflowId", source = "workflowId")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "success", source = "success")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    com.example.temporal.common.dto.TransferControlResponse toControlResponse(
            ControlTransferUseCase.ControlResult result);
}
