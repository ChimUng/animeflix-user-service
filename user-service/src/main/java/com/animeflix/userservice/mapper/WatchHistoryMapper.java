package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.response.WatchHistoryResponse;
import com.animeflix.userservice.entity.WatchHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WatchHistoryMapper {

    @Mapping(target = "anime", ignore = true)  // Sẽ set manually từ catalog-service
    WatchHistoryResponse toResponse(WatchHistory entity);
}