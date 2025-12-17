package com.animeflix.authservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "developers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Developer {
    @Id
    private String id;
    @Indexed(unique = true)
    private String appId;
    private String appName;
    private String username;
    private String email;
    private String passwordHash;
    private String clientId;
    private String clientSecret;
    private String apiKey;          // MAIN KEY for dev
    private int rateLimit = 1000;
    private boolean isActive = true;
    private String provider = "animeflix";
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public void setIsActive(boolean b) {
    }
}