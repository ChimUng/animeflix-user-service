package com.animeflix.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.anime-catalog.url}")
    private String animeCatalogUrl;

    @Value("${services.auth.url}")
    private String authServiceUrl;

    @Bean(name = "animeCatalogWebClient")
    @Primary
    public WebClient animeCatalogWebClient() {
        return WebClient.builder()
                .baseUrl(animeCatalogUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean(name = "authServiceWebClient")
    public WebClient authServiceWebClient() {
        return WebClient.builder()
                .baseUrl(authServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
