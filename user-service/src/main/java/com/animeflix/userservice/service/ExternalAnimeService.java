package com.animeflix.userservice.service;

import com.animeflix.userservice.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalAnimeService {

    @Qualifier("animeCatalogWebClient")
    private final WebClient animeCatalogClient;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String CACHE_PREFIX = "anime:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * Lấy thông tin cơ bản của anime (cho history, continue-watching)
     */
    public Mono<AnimeBasicInfo> getAnimeBasicInfo(String animeId) {
        String cacheKey = CACHE_PREFIX + animeId + ":basic";

        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(cached -> {
                    log.debug("Cache hit for anime basic info: {}", animeId);
                    return parseBasicInfo(cached);
                })
                .switchIfEmpty(fetchAnimeBasicInfo(animeId))
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch anime info for {}: {}", animeId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<AnimeBasicInfo> fetchAnimeBasicInfo(String animeId) {
        return animeCatalogClient.get()
                .uri("/{id}", animeId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.path("data");
                    return AnimeBasicInfo.builder()
                            .id(data.path("id").asText())
                            .title(data.path("title").path("userPreferred").asText())
                            .coverImage(data.path("coverImage").path("large").asText())
                            .bannerImage(data.path("bannerImage").asText())
                            .totalEpisodes(data.path("episodes").asInt())
                            .status(data.path("status").asText())
                            .format(data.path("format").asText())
                            .build();
                })
                .doOnNext(info -> {
                    // Cache result (async)
                    cacheAnimeInfo(CACHE_PREFIX + animeId + ":basic", info)
                            .subscribe();
                });
    }

    /**
     * Lấy chi tiết đầy đủ của anime (cho favorites)
     */
    public Mono<AnimeDetails> getAnimeDetails(String animeId) {
        return animeCatalogClient.get()
                .uri("/{id}", animeId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.path("data");
                    return AnimeDetails.builder()
                            .title(data.path("title").path("userPreferred").asText())
                            .coverImage(data.path("coverImage").path("large").asText())
                            .bannerImage(data.path("bannerImage").asText())
                            .status(data.path("status").asText())
                            .totalEpisodes(data.path("episodes").asInt())
                            .build();
                })
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("Failed to fetch anime details: {}", e.getMessage());
                    return Mono.error(new ExternalServiceException(
                            "Failed to fetch anime details", e));
                });
    }

    private Mono<AnimeBasicInfo> parseBasicInfo(String json) {
        // TODO: Parse JSON từ cache
        return Mono.empty();
    }

    private Mono<Void> cacheAnimeInfo(String key, AnimeBasicInfo info) {
        // TODO: Serialize và cache
        return Mono.empty();
    }

    // DTO cho anime basic info
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnimeBasicInfo {
        private String id;
        private String title;
        private String coverImage;
        private String bannerImage;
        private Integer totalEpisodes;
        private String status;
        private String format;
    }

    // DTO cho anime details
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnimeDetails {
        private String title;
        private String coverImage;
        private String bannerImage;
        private String status;
        private Integer totalEpisodes;
    }
}