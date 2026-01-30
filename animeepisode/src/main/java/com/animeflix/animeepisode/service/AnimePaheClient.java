package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Includes smart title matching algorithm (Levenshtein Distance)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnimePaheClient {

    private final WebClient consumetWebClient;

    /**
     * Main fetch method
     */
    public Mono<Provider> fetchAnimePahe(
            String anilistId,
            String title,
            Integer year,
            String type
    ) {
        log.info("🔍 Searching AnimePahe for: \"{}\" ({})", title, year);

        return searchAnimePahe(title, year, type)
                .flatMap(uuid -> {
                    if (uuid == null) {
                        log.warn("❌ Could not find AnimePahe ID for \"{}\"", title);
                        return Mono.just(emptyProvider());
                    }

                    log.info("✅ AnimePahe UUID: {}", uuid);
                    return fetchEpisodes(uuid);
                })
                .onErrorResume(e -> {
                    log.error("❌ AnimePahe error for \"{}\": {}", title, e.getMessage());
                    return Mono.just(emptyProvider());
                });
    }

    /**
     * ✅ STEP 1: Search AnimePahe by title
     */
    private Mono<String> searchAnimePahe(String title, Integer year, String type) {
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String uri = "/anime/animepahe/" + encodedTitle;

        return consumetWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    JsonNode resultsNode = response.path("results");
                    if (!resultsNode.isArray() || resultsNode.isEmpty()) {
                        log.warn("⚠️ AnimePahe: No results for \"{}\"", title);
                        return null;
                    }

                    // Parse results
                    List<SearchResult> results = new ArrayList<>();
                    resultsNode.forEach(node -> {
                        SearchResult result = new SearchResult();
                        result.setId(node.path("id").asText());
                        result.setTitle(node.path("title").asText());
                        result.setType(node.path("type").asText());
                        result.setReleaseDate(node.path("releaseDate").asInt(0));
                        results.add(result);
                    });

                    // Find best match
                    SearchResult bestMatch = findBestMatch(results, title, year, type);
                    return bestMatch != null ? bestMatch.getId() : null;
                })
                .onErrorResume(e -> {
                    log.error("❌ Search error: {}", e.getMessage());
                    return Mono.just(null);
                });
    }

    /**
     * ✅ STEP 2: Fetch episodes from AnimePahe
     */
    private Mono<Provider> fetchEpisodes(String uuid) {
        String uri = "/anime/animepahe/info/" + uuid;

        return consumetWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    JsonNode episodesNode = response.path("episodes");

                    if (!episodesNode.isArray() || episodesNode.isEmpty()) {
                        log.warn("⚠️ No episodes array for AnimePahe UUID: {}", uuid);
                        return emptyProvider();
                    }

                    List<Episode> episodes = new ArrayList<>();
                    episodesNode.forEach(epNode -> {
                        Episode ep = new Episode();
                        ep.setId(epNode.path("id").asText()); // Full ID with hash
                        ep.setNumber(epNode.path("number").asInt());
                        ep.setTitle(epNode.path("title").asText("Episode " + epNode.path("number").asInt()));
                        episodes.add(ep);
                    });

                    log.info("✅ AnimePahe: Found {} episodes", episodes.size());
                    return new Provider("animepahe", "animepahe", false, episodes);
                });
    }

    // ========================================
    // SMART MATCHING ALGORITHM
    // ========================================

    /**
     * ✅ Find best match from search results
     * Scoring weights:
     * - Title similarity: 70%
     * - Type match: 15%
     * - Year proximity: 10%
     * - TV priority: 5%
     */
    private SearchResult findBestMatch(
            List<SearchResult> results,
            String targetTitle,
            Integer targetYear,
            String targetType
    ) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        // Calculate scores
        List<ScoredMatch> scored = results.stream()
                .map(result -> {
                    double score = 0.0;

                    // 1. Title similarity (70%)
                    double titleScore = calculateSimilarity(result.getTitle(), targetTitle);
                    score += titleScore * 0.7;

                    // 2. Type match (15%)
                    if (targetType != null &&
                            targetType.equalsIgnoreCase(result.getType())) {
                        score += 0.15;
                    }

                    // 3. Year proximity (10%)
                    if (targetYear != null && result.getReleaseDate() > 0) {
                        int yearDiff = Math.abs(result.getReleaseDate() - targetYear);
                        double yearScore = Math.max(0, 1 - yearDiff / 10.0); // Max 10 years diff
                        score += yearScore * 0.1;
                    }

                    // 4. TV priority (5%)
                    if ("TV".equalsIgnoreCase(result.getType())) {
                        score += 0.05;
                    }

                    return new ScoredMatch(result, score);
                })
                .sorted(Comparator.comparingDouble(ScoredMatch::getScore).reversed())
                .collect(Collectors.toList());

        // Log top 3 matches
        log.info("📊 AnimePahe search matches:");
        scored.stream().limit(3).forEach(match ->
                log.info("  - \"{}\" ({}) - Score: {:.1f}%",
                        match.getResult().getTitle(),
                        match.getResult().getType(),
                        match.getScore() * 100
                )
        );

        // Accept if score > 60%
        if (scored.get(0).getScore() > 0.6) {
            log.info("✅ Best match: \"{}\" ({:.1f}%)",
                    scored.get(0).getResult().getTitle(),
                    scored.get(0).getScore() * 100
            );
            return scored.get(0).getResult();
        }

        log.warn("⚠️ No good match found (best: {:.1f}%)",
                scored.get(0).getScore() * 100
        );
        return null;
    }

    /**
     * ✅ Calculate similarity between two strings (Levenshtein Distance)
     * Returns: 0.0 to 1.0 (1.0 = exact match)
     */
    private double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) return 0.0;

        String s1 = str1.toLowerCase().trim();
        String s2 = str2.toLowerCase().trim();

        // Exact match
        if (s1.equals(s2)) return 1.0;

        // Normalize: remove special chars, extra spaces
        String n1 = normalize(s1);
        String n2 = normalize(s2);

        if (n1.equals(n2)) return 0.95;

        // Check contains
        if (n1.contains(n2) || n2.contains(n1)) return 0.85;

        // Levenshtein distance
        int len1 = n1.length();
        int len2 = n2.length();
        int[][] matrix = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) matrix[i][0] = i;
        for (int j = 0; j <= len2; j++) matrix[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = n1.charAt(i - 1) == n2.charAt(j - 1) ? 0 : 1;
                matrix[i][j] = Math.min(
                        Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1),
                        matrix[i - 1][j - 1] + cost
                );
            }
        }

        int maxLen = Math.max(len1, len2);
        return 1.0 - (double) matrix[len1][len2] / maxLen;
    }

    private String normalize(String str) {
        return str.replaceAll("[^\\w\\s]", "")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();
    }

    private Provider emptyProvider() {
        return new Provider("animepahe", "animepahe", false, new ArrayList<>());
    }

    // ========================================
    // INNER CLASSES
    // ========================================

    @Data
    private static class SearchResult {
        private String id;
        private String title;
        private String type;
        private int releaseDate;
    }

    @Data
    @AllArgsConstructor
    private static class ScoredMatch {
        private SearchResult result;
        private double score;
    }
}