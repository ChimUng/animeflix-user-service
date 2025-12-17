package com.animeflix.authservice.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private String userId;
    private String username;
    private String email;
    private String avatar;
    private String provider;
    private LocalDateTime createdAt;
    private String message;
}