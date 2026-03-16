package com.example.temporal.account.infrastructure.adapter.in.rest.mapper;

import com.example.temporal.account.domain.model.AccountDomain;
import com.example.temporal.account.domain.port.in.CreateAccountUseCase;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.AccountCreateRequest;
import com.example.temporal.account.infrastructure.adapter.in.rest.dto.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * Mapper for Account REST API - converts between DTOs and domain objects
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountRestMapper {

    /**
     * Converts AccountDomain to AccountResponse DTO
     */
    AccountResponse toAccountResponse(AccountDomain domain);

    /**
     * Converts list of AccountDomain to list of AccountResponse DTOs
     */
    List<AccountResponse> toAccountResponses(List<AccountDomain> domains);

    /**
     * Converts AccountCreateRequest to CreateAccountCommand
     */
    @Mapping(target = "initialBalance", source = "balance")
    CreateAccountUseCase.CreateAccountCommand toCreateAccountCommand(
            AccountCreateRequest request);
}
