package com.animeflix.authservice.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {
    private String id;
    private String username;
    private String email;
    private String avatar;
    private String provider;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean active;
}