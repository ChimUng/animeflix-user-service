package com.animeflix.aisearchservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Lấy genre preference của user từ watch history.
 * Cache vào Redis để tránh gọi user-service nhiều lần.
 *
 * Genre preference map: { "Action": 5, "Comedy": 3, "Romance": 2 }
 * (count = số anime thuộc genre đó user đã xem)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserHistoryService {

    private final WebClient userServiceWebClient;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "ai:user:genre:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * Trả về map genre → count (số lần user xem anime thuộc genre đó).
     * Dùng để tính personalized score trong rerank.
     */
    public Mono<Map<String, Integer>> getGenrePreference(String userId) {
        if (userId == null || userId.isBlank()) {
            return Mono.just(new HashMap<>());
        }

        String cacheKey = CACHE_PREFIX + userId;

        // Kiểm tra cache trước
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Mono.just(parseGenreMap(cached));
        }

        // Gọi user-service lấy watch history → tính genre count
        return userServiceWebClient.get()
                .uri("/api/user/history")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    Map<String, Integer> genreCount = new HashMap<>();
                    JsonNode historyList = response.path("data");

                    if (historyList.isArray()) {
                        historyList.elements().forEachRemaining(item -> {
                            JsonNode genres = item.path("genres");
                            if (genres.isArray()) {
                                genres.elements().forEachRemaining(g -> {
                                    String genre = g.asText();
                                    genreCount.merge(genre, 1, Integer::sum);
                                });
                            }
                        });
                    }

                    // Cache kết quả
                    try {
                        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(genreCount);
                        redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
                    } catch (Exception e) {
                        log.warn("⚠️ Cannot cache genre preference: {}", e.getMessage());
                    }

                    log.debug("✅ Genre preference for user {}: {}", userId, genreCount);
                    return genreCount;
                })
                .onErrorReturn(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseGenreMap(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}