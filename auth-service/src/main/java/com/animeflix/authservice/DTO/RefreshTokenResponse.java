package com.animeflix.authservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    private LocalDateTime expiresAt;
}

