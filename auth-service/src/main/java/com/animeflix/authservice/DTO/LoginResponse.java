package com.animeflix.authservice.DTO;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private long expiresIn = 3600;
    @Builder.Default
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private String email;
    private LocalDateTime expiresAt;
}