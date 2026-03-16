package com.example.temporal.notification.infrastructure.adapter.out.persistence;

import com.example.temporal.notification.domain.model.NotificationDomain;
import com.example.temporal.notification.entity.NotificationEntity;
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
public interface NotificationMapper {

    /**
     * Converts domain model to JPA entity
     */
    @Mapping(target = "notificationStatus", source = "status", qualifiedByName = "toEntityStatus")
    NotificationEntity toEntity(NotificationDomain domain);

    /**
     * Converts JPA entity to domain model
     */
    @Mapping(target = "status", source = "notificationStatus", qualifiedByName = "toDomainStatus")
    NotificationDomain toDomain(NotificationEntity entity);

    /**
     * Converts domain status to entity status
     */
    @Named("toEntityStatus")
    default NotificationEntity.NotificationStatus toEntityStatus(
            NotificationDomain.NotificationStatus domainStatus) {
        if (domainStatus == null) {
            return null;
        }
        return NotificationEntity.NotificationStatus.valueOf(domainStatus.name());
    }

    /**
     * Converts entity status to domain status
     */
    @Named("toDomainStatus")
    default NotificationDomain.NotificationStatus toDomainStatus(
            NotificationEntity.NotificationStatus entityStatus) {
        if (entityStatus == null) {
            return null;
        }
        return NotificationDomain.NotificationStatus.valueOf(entityStatus.name());
    }
}
