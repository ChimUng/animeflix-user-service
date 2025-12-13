package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.response.NotificationResponse;
import com.animeflix.userservice.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "type", source = "type")
    NotificationResponse toResponse(Notification entity);

    default String map(Notification.NotificationType type) {
        return type != null ? type.name() : null;
    }
}