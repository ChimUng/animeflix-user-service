package com.animeflix.authservice.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RegisterDevResponse {
    private String clientId;
    private String clientSecret;
    private String apiKey;

    private String message;
}