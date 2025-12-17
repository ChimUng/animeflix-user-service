package com.animeflix.authservice.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
}