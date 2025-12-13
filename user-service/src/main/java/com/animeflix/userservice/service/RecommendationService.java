package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.response.RecommendationResponse;
import com.animeflix.userservice.repository.WatchHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final WatchHistoryRepository historyRepo;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Qualifier("animeCatalogWebClient")
    private final WebClient animeCatalogClient;

    private static final String CACHE_KEY_PREFIX = "recommendations:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    /**
     * Lấy gợi ý anime cho user
     */
    public Mono<RecommendationResponse> getRecommendations(String userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        // Try cache first
        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(cached -> {
                    log.debug("Cache hit for recommendations: {}", userId);
                    return Mono.empty(); // Parse JSON if needed
                })
                .switchIfEmpty(Mono.defer(() -> generateRecommendations(userId)))
                .doOnNext(response -> {
                    // Cache result (async)
                    cacheRecommendations(cacheKey, response)
                            .subscribe(
                                    v -> log.debug("Cached recommendations for: {}", userId),
                                    e -> log.warn("Failed to cache recommendations: {}", e.getMessage())
                            );
                });
    }

    private Mono<RecommendationResponse> generateRecommendations(String userId) {
        log.info("Generating recommendations for user: {}", userId);

        return historyRepo.findTop20ByUserIdOrderByWatchedAtDesc(userId)
                .collectList()
                .flatMap(history -> {
                    if (history.isEmpty()) {
                        // Nếu chưa xem gì → Return trending anime
                        return getTrendingAnime();
                    }

                    // Phân tích genres từ history
                    return analyzeWatchHistory(history)
                            .flatMap(this::findSimilarAnime);
                });
    }

    private Mono<Map<String, Integer>> analyzeWatchHistory(List<?> history) {
        // TODO: Implement genre analysis từ history
        // Cần fetch anime details từ catalog-service để lấy genres

        // Simplified version:
        Map<String, Integer> genreScores = new HashMap<>();
        genreScores.put("Action", 10);
        genreScores.put("Comedy", 5);

        return Mono.just(genreScores);
    }

    private Mono<RecommendationResponse> findSimilarAnime(Map<String, Integer> genreScores) {
        // Lấy top 3 genres
        List<String> topGenres = genreScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("Top genres for recommendations: {}", topGenres);

        // Gọi anime-catalog-service để search anime theo genres
        return animeCatalogClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("genres", String.join(",", topGenres))
                        .queryParam("sort", "POPULARITY_DESC")
                        .queryParam("page", 1)
                        .queryParam("perPage", 10)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    // Parse response và tạo RecommendationResponse
                    List<RecommendationResponse.AnimeRecommendation> recommendations = new ArrayList<>();

                    JsonNode data = response.path("data");
                    if (data.isArray()) {
                        data.forEach(node -> {
                            recommendations.add(RecommendationResponse.AnimeRecommendation.builder()
                                    .id(node.path("id").asText())
                                    .title(node.path("title").path("userPreferred").asText())
                                    .coverImage(node.path("coverImage").path("large").asText())
                                    .score(calculateScore(node, genreScores))
                                    .matchReason(getMatchReason(topGenres))
                                    .build());
                        });
                    }

                    return RecommendationResponse.builder()
                            .recommendations(recommendations)
                            .reason("Based on your watch history")
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch recommendations: {}", e.getMessage());
                    return getTrendingAnime(); // Fallback
                });
    }

    private Mono<RecommendationResponse> getTrendingAnime() {
        return animeCatalogClient.get()
                .uri("/trending?page=1&perPage=10")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    List<RecommendationResponse.AnimeRecommendation> recommendations = new ArrayList<>();

                    JsonNode data = response.path("data");
                    if (data.isArray()) {
                        data.forEach(node -> {
                            recommendations.add(RecommendationResponse.AnimeRecommendation.builder()
                                    .id(node.path("id").asText())
                                    .title(node.path("title").path("userPreferred").asText())
                                    .coverImage(node.path("coverImage").path("large").asText())
                                    .score(80)
                                    .matchReason("Trending now")
                                    .build());
                        });
                    }

                    return RecommendationResponse.builder()
                            .recommendations(recommendations)
                            .reason("Trending anime - Start watching to get personalized recommendations")
                            .build();
                });
    }

    private Integer calculateScore(JsonNode node, Map<String, Integer> genreScores) {
        // Simple scoring: base popularity + genre match bonus
        int baseScore = node.path("popularity").asInt(0) / 1000;

        JsonNode genres = node.path("genres");
        int genreBonus = 0;
        if (genres.isArray()) {
            for (JsonNode genre : genres) {
                genreBonus += genreScores.getOrDefault(genre.asText(), 0);
            }
        }

        return Math.min(100, baseScore + genreBonus);
    }

    private String getMatchReason(List<String> topGenres) {
        if (topGenres.isEmpty()) return "Popular choice";
        return "Matches your favorite genres: " + String.join(", ", topGenres);
    }

    private Mono<Void> cacheRecommendations(String key, RecommendationResponse response) {
        // TODO: Serialize to JSON and cache
        return Mono.empty();
    }
}