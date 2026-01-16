package com.animeflix.animetranslate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TranslateConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon.key}")
    private String supabaseAnonKey;

    @Value("${supabase.service.key}")
    private String supabaseServiceKey;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient supabaseWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("apikey", supabaseAnonKey)
                .defaultHeader("Authorization", "Bearer " + supabaseAnonKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient supabaseServiceWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("apikey", supabaseServiceKey)
                .defaultHeader("Authorization", "Bearer " + supabaseServiceKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Prefer", "resolution=merge-duplicates")
                .build();
    }

    // Getters for keys (if needed in service)
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getSupabaseServiceKey() { return supabaseServiceKey; }
}