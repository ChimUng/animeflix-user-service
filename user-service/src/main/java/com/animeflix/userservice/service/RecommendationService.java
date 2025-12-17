package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.response.RecommendationResponse;
import com.animeflix.userservice.entity.WatchHistory;
import com.animeflix.userservice.repository.WatchHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("animeCatalogWebClient")
    private final WebClient animeCatalogClient;

    private static final String CACHE_KEY_PREFIX = "recommendations:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    /**
     * ✅ PUBLIC API - Lấy gợi ý anime cho user
     */
    public Mono<RecommendationResponse> getRecommendations(String userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;

        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(this::parseFromCache)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("🔍 Generating recommendations for user: {}", userId);
                    return generateRecommendations(userId)
                            .doOnNext(response -> cacheRecommendations(cacheKey, response)
                                    .subscribe());
                }));
    }

    /**
     * ✅ STEP 1: Generate recommendations
     */
    private Mono<RecommendationResponse> generateRecommendations(String userId) {
        return historyRepo.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .collectList()
                .flatMap(history -> {
                    if (history.isEmpty()) {
                        log.info("📺 No watch history - Returning trending anime");
                        return getTrendingAnime();
                    }

                    // Lấy top 5 anime gần nhất
                    List<String> recentAnimeIds = history.stream()
                            .limit(5)
                            .map(WatchHistory::getAniId)
                            .distinct()
                            .collect(Collectors.toList());

                    log.debug("🎬 Recent anime IDs: {}", recentAnimeIds);

                    // ✅ FIX: Fetch anime details từ CATALOG SERVICE
                    return analyzeWatchHistory(recentAnimeIds)
                            .flatMap(this::findSimilarAnime);
                });
    }

    /**
     * ✅ STEP 2: Phân tích watch history - FIX CHỖ NÀY
     * Gọi CATALOG SERVICE để lấy genres của các anime đã xem
     */
    private Mono<Map<String, Integer>> analyzeWatchHistory(List<String> animeIds) {
        log.debug("🔍 Analyzing {} anime from watch history", animeIds.size());

        // Fetch anime details từ catalog service để lấy genres
        return Flux.fromIterable(animeIds)
                .flatMap(animeId -> {
                    log.debug("📡 Fetching anime details: {}", animeId);

                    return animeCatalogClient.get()
                            .uri("/{id}", animeId)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                // Parse genres từ response
                                JsonNode media = response.path("data").path("Media");
                                JsonNode genresNode = media.path("genres");

                                List<String> genres = new ArrayList<>();
                                if (genresNode.isArray()) {
                                    genresNode.forEach(g -> genres.add(g.asText()));
                                }

                                log.debug("✅ Genres for anime {}: {}", animeId, genres);
                                return genres;
                            })
                            .onErrorResume(e -> {
                                log.warn("⚠️ Failed to fetch anime {}: {}", animeId, e.getMessage());
                                return Mono.just(Collections.emptyList());
                            });
                }, 3) // Fetch 3 anime đồng thời
                .flatMap(Flux::fromIterable) // Flatten List<String> to String
                .collectMultimap(genre -> genre, genre -> 1) // Count genres
                .map(multimap -> {
                    // Convert MultiValueMap to Map<String, Integer>
                    Map<String, Integer> genreScores = multimap.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().size()
                            ));

                    log.info("🎯 Genre analysis: {}", genreScores);
                    return genreScores;
                })
                .defaultIfEmpty(Collections.emptyMap());
    }

    /**
     * ✅ STEP 3: Tìm anime tương tự dựa trên genres
     */
    private Mono<RecommendationResponse> findSimilarAnime(Map<String, Integer> genreScores) {
        if (genreScores.isEmpty()) {
            log.warn("⚠️ No genres found - Fallback to trending");
            return getTrendingAnime();
        }

        // Top 3 genres yêu thích
        List<String> topGenres = genreScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("🎯 Top genres: {}", topGenres);

        // ✅ Gọi TRENDING từ catalog service (đã có genres)
        return getTrendingAnime()
                .map(response -> {
                    // Score lại dựa trên genre matching
                    List<RecommendationResponse.AnimeRecommendation> scored =
                            response.getRecommendations().stream()
                                    .map(anime -> {
                                        int score = calculateScore(anime.getGenres(), genreScores);
                                        anime.setScore(score);
                                        anime.setMatchReason(getMatchReason(topGenres));
                                        return anime;
                                    })
                                    .filter(anime -> anime.getScore() > 0)
                                    .sorted(Comparator.comparingInt(
                                                    RecommendationResponse.AnimeRecommendation::getScore)
                                            .reversed())
                                    .limit(10)
                                    .collect(Collectors.toList());

                    return RecommendationResponse.builder()
                            .recommendations(scored)
                            .reason("Based on your watch history: " + String.join(", ", topGenres))
                            .build();
                });
    }

    /**
     * ✅ STEP 4: Lấy trending anime từ CATALOG SERVICE
     * Response đã có genres, chỉ cần parse
     */
    private Mono<RecommendationResponse> getTrendingAnime() {
        log.debug("📈 Fetching trending anime from catalog service");

        return animeCatalogClient.get()
                .uri("/trending?page=1&perPage=20")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    List<RecommendationResponse.AnimeRecommendation> recommendations = new ArrayList<>();

                    // Parse response từ catalog service
                    JsonNode data = response.path("data");
                    JsonNode pageData = data.path("Page");
                    JsonNode mediaList = pageData.path("media");

                    if (mediaList.isArray()) {
                        mediaList.forEach(node -> {
                            // Parse genres
                            List<String> genres = new ArrayList<>();
                            JsonNode genresNode = node.path("genres");
                            if (genresNode.isArray()) {
                                genresNode.forEach(g -> genres.add(g.asText()));
                            }

                            recommendations.add(RecommendationResponse.AnimeRecommendation.builder()
                                    .id(node.path("id").asText())
                                    .title(node.path("title").path("userPreferred").asText())
                                    .coverImage(node.path("coverImage").path("large").asText())
                                    .bannerImage(node.path("bannerImage").asText(null))
                                    .genres(genres)
                                    .averageScore(node.path("averageScore").asInt(0))
                                    .popularity(node.path("popularity").asInt(0))
                                    .status(node.path("status").asText())
                                    .format(node.path("format").asText())
                                    .score(80) // Default score, sẽ được recalculate
                                    .matchReason("Trending now")
                                    .build());
                        });
                    }

                    log.info("✅ Fetched {} trending anime", recommendations.size());

                    return RecommendationResponse.builder()
                            .recommendations(recommendations)
                            .reason("Trending anime - Start watching to get personalized recommendations")
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("❌ Failed to fetch trending anime: {}", e.getMessage());
                    return Mono.just(RecommendationResponse.builder()
                            .recommendations(Collections.emptyList())
                            .reason("Unable to fetch recommendations")
                            .build());
                });
    }

    /**
     * ✅ HELPER: Calculate score dựa trên genre matching
     */
    private Integer calculateScore(List<String> animeGenres, Map<String, Integer> genreScores) {
        if (animeGenres == null || animeGenres.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (String genre : animeGenres) {
            score += genreScores.getOrDefault(genre, 0) * 10; // x10 để score rõ ràng hơn
        }

        return score;
    }

    /**
     * ✅ HELPER: Generate match reason
     */
    private String getMatchReason(List<String> topGenres) {
        if (topGenres.isEmpty()) {
            return "Popular choice";
        }
        return "Matches your favorite genres: " + String.join(", ", topGenres);
    }

    // ========== CACHE HELPERS ==========

    private Mono<RecommendationResponse> parseFromCache(String cachedJson) {
        try {
            RecommendationResponse response = objectMapper.readValue(
                    cachedJson, RecommendationResponse.class);
            log.debug("✅ Cache hit");
            return Mono.just(response);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse cache: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<Boolean> cacheRecommendations(String key, RecommendationResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            return redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to cache: {}", e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * ✅ PUBLIC API - Clear cache cho user (optional feature)
     */
    public Mono<Void> clearCache(String userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId;
        log.info("🗑️ Clearing cache for user: {}", userId);

        return redisTemplate.delete(cacheKey)
                .doOnSuccess(deleted -> {
                    if (deleted > 0) {
                        log.info("✅ Cache cleared for user: {}", userId);
                    } else {
                        log.debug("ℹ️ No cache found for user: {}", userId);
                    }
                })
                .then();
    }
}