package com.animeflix.authservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    private String id;
    private String userId;
    private String accessToken;
    @Indexed(unique = true)
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String device;
    private String ip;
    private boolean isRevoked = false;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}