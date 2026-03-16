package com.example.temporal.account.infrastructure.adapter.out.persistence;

import com.example.temporal.account.domain.model.AccountDomain;
import com.example.temporal.common.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper between domain model and JPA entity
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountMapper {

    AccountDomain toDomain(Account entity);

    Account toEntity(AccountDomain domain);

    void updateEntity(@MappingTarget Account entity, AccountDomain domain);
}
