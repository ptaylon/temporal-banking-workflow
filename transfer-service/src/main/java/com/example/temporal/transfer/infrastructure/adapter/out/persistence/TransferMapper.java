package com.example.temporal.transfer.infrastructure.adapter.out.persistence;

import com.example.temporal.common.model.Transfer;
import com.example.temporal.transfer.domain.model.TransferDomain;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper between domain model and JPA entity
 * Isolates domain from persistence technology
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransferMapper {

    /**
     * Convert JPA entity to domain model
     */
    TransferDomain toDomain(Transfer entity);

    /**
     * Convert domain model to JPA entity
     */
    Transfer toEntity(TransferDomain domain);

    /**
     * Update existing entity with domain data
     */
    void updateEntity(@MappingTarget Transfer entity, TransferDomain domain);
}
