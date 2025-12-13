package com.animeflix.userservice.mapper;

import com.animeflix.userservice.dto.response.FavoriteResponse;
import com.animeflix.userservice.entity.Favorite;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    FavoriteResponse toResponse(Favorite entity);
}