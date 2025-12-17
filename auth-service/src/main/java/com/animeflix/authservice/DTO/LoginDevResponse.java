package com.animeflix.authservice.DTO;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginDevResponse {
    private String appName;
    private String username;
    private String email;
    private String clientId;
    private String clientSecret;
    private String apiKey;
    private int rateLimit = 1000;
    private boolean isActive = true;
    private String provider = "animeflix";
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private String message;
}
