package com.animeflix.authservice.mapper;

import com.animeflix.authservice.DTO.ProfileResponse;
import com.animeflix.authservice.DTO.SignupRequest;
import com.animeflix.authservice.DTO.SignupResponse;
import com.animeflix.authservice.Entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "provider", defaultValue = "animeflix")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    User toEntity(SignupRequest request);

    @Mapping(target = "message", constant = "Signup successful")
    SignupResponse toResponse(User user);

    ProfileResponse toProfileResponse(User user);
}