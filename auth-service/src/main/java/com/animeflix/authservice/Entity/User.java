package com.animeflix.authservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String username;
    @Indexed(unique = true)
    private String email;
    private String passwordHash;
    private String avatar;
    private String provider;
    private String providerId;
    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public void setIsActive(boolean b) {

    }
}