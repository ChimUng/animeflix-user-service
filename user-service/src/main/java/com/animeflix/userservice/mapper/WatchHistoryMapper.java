package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.response.WatchHistoryResponse;
import com.animeflix.userservice.entity.WatchHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WatchHistoryMapper {

    WatchHistoryResponse toResponse(WatchHistory entity);
}