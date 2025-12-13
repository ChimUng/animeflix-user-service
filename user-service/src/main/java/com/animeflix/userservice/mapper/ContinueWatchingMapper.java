package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.response.ContinueWatchingResponse;
import com.animeflix.userservice.entity.ContinueWatching;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContinueWatchingMapper {

    ContinueWatchingResponse toResponse(ContinueWatching entity);
}