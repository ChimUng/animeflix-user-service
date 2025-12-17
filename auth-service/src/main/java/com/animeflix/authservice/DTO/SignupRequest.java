package com.animeflix.authservice.DTO;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class SignupRequest {
    private String username;
    private String email;
    private String password;

    @Builder.Default
    private String provider = "animeflix";
}
