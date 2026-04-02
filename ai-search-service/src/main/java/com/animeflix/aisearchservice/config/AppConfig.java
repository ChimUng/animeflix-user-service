package com.animeflix.aisearchservice.config;

import com.animeflix.aisearchservice.client.GeminiClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${gemini.api.base-url}")
    private String geminiBaseUrl;

    @Value("${services.anime-catalog.url}")
    private String catalogUrl;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${anilist.api.url}")
    private String aniListUrl;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
    }

    @Bean
    public WebClient catalogWebClient() {
        return WebClient.builder()
                .baseUrl(catalogUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient aniListWebClient() {
        return WebClient.builder()
                .baseUrl(aniListUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}