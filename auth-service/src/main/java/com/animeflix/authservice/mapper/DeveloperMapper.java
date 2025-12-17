package com.animeflix.authservice.mapper;

import com.animeflix.authservice.DTO.*;
import com.animeflix.authservice.Entity.Developer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DeveloperMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "clientSecret", ignore = true)
    @Mapping(target = "apiKey", ignore = true)
    @Mapping(target = "rateLimit", constant = "1000")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "provider", constant = "animeflix")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Developer toEntity(RegisterDevRequest request);

    @Mapping(target = "message", constant = "Register developer successful")
    RegisterDevResponse toRegisterResponse(Developer developer);

    @Mapping(target = "message", constant = "Login developer successful")
    LoginDevResponse toLoginResponse(Developer developer);
}
