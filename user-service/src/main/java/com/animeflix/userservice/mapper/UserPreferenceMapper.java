package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.request.UpdatePreferencesRequest;
import com.animeflix.userservice.dto.response.UserPreferenceResponse;
import com.animeflix.userservice.entity.UserPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserPreferenceMapper {

    UserPreferenceResponse toResponse(UserPreference entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePreferencesRequest request, @MappingTarget UserPreference entity);
}