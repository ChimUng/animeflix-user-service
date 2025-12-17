package com.animeflix.authservice.DTO;

import lombok.Data;

@Data
public class RegisterDevRequest {
    private String appId;
    private String appName;
    private String username;
    private String email;
    private String password;
}
