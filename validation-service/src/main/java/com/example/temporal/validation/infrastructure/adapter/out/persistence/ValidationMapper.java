package com.example.temporal.validation.infrastructure.adapter.out.persistence;

import com.example.temporal.validation.domain.model.TransferValidationDomain;
import com.example.temporal.validation.entity.TransferValidationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper between domain model and JPA entity
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ValidationMapper {

    /**
     * Converts domain model to JPA entity
     */
    @Mapping(target = "validationResult", source = "validationResult", qualifiedByName = "toEntityValidationResult")
    TransferValidationEntity toEntity(TransferValidationDomain domain);

    /**
     * Converts JPA entity to domain model
     */
    @Mapping(target = "validationResult", source = "validationResult", qualifiedByName = "toDomainValidationResult")
    TransferValidationDomain toDomain(TransferValidationEntity entity);

    /**
     * Converts domain validation result to entity validation result
     */
    @Named("toEntityValidationResult")
    default TransferValidationEntity.ValidationResult toEntityValidationResult(
            TransferValidationDomain.ValidationResult domainResult) {
        if (domainResult == null) {
            return null;
        }
        return TransferValidationEntity.ValidationResult.valueOf(domainResult.name());
    }

    /**
     * Converts entity validation result to domain validation result
     */
    @Named("toDomainValidationResult")
    default TransferValidationDomain.ValidationResult toDomainValidationResult(
            TransferValidationEntity.ValidationResult entityResult) {
        if (entityResult == null) {
            return null;
        }
        return TransferValidationDomain.ValidationResult.valueOf(entityResult.name());
    }
}
