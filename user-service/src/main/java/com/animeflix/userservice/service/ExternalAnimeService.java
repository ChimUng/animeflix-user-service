package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.response.WatchHistoryResponse;
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
    public Mono<WatchHistoryResponse.AnimeInfo> getAnimeBasicInfo(String animeId) {
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

    private Mono<WatchHistoryResponse.AnimeInfo> fetchAnimeBasicInfo(String animeId) {
        return animeCatalogClient.get()
                .uri("/{id}", animeId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode data = response.path("data");
                    return WatchHistoryResponse.AnimeInfo.builder()
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
                    return new AnimeDetails(
                            data.path("title").path("userPreferred").asText(),
                            data.path("coverImage").path("large").asText(),
                            data.path("bannerImage").asText(),
                            data.path("status").asText(),
                            data.path("episodes").asInt()
                    );
                })
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("Failed to fetch anime details: {}", e.getMessage());
                    return Mono.error(new ExternalServiceException(
                            "Failed to fetch anime details", e));
                });
    }

    private Mono<WatchHistoryResponse.AnimeInfo> parseBasicInfo(String json) {
        // TODO: Parse JSON từ cache
        return Mono.empty();
    }

    private Mono<Void> cacheAnimeInfo(String key, WatchHistoryResponse.AnimeInfo info) {
        // TODO: Serialize và cache
        return Mono.empty();
    }

    // DTO cho anime details
    public record AnimeDetails(
            String title,
            String coverImage,
            String bannerImage,
            String status,
            Integer totalEpisodes
    ) {}
}