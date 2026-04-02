package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.dto.response.AnimeSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Re-ranking service: tính final score cho mỗi anime trước khi trả về FE.
 *
 * STRUCTURED path (Query Parser):
 *   finalScore = 0.35 * popularity + 0.30 * score + 0.20 * trending + 0.15 * personalized
 *
 * SEMANTIC path (Embedding):
 *   finalScore = 0.50 * cosine + 0.25 * popularity + 0.15 * score + 0.10 * personalized
 */
@Service
@Slf4j
public class ReRankService {

    // Weights từ config (structured path)
    @Value("${search.rerank.weights.structured.popularity:0.35}")
    private double wStructuredPopularity;
    @Value("${search.rerank.weights.structured.score:0.30}")
    private double wStructuredScore;
    @Value("${search.rerank.weights.structured.trending:0.20}")
    private double wStructuredTrending;
    @Value("${search.rerank.weights.structured.personalized:0.15}")
    private double wStructuredPersonalized;

    // Weights từ config (semantic path)
    @Value("${search.rerank.weights.semantic.cosine-similarity:0.50}")
    private double wSemanticCosine;
    @Value("${search.rerank.weights.semantic.popularity:0.25}")
    private double wSemanticPopularity;
    @Value("${search.rerank.weights.semantic.score:0.15}")
    private double wSemanticScore;
    @Value("${search.rerank.weights.semantic.personalized:0.10}")
    private double wSemanticPersonalized;

    // Max values để normalize
    private static final double MAX_POPULARITY = 500_000.0;
    private static final double MAX_SCORE = 100.0;

    /**
     * Rerank kết quả từ Query Parser path (AniList API).
     */
    public List<AnimeSearchResultDTO> rerankStructured(
            List<AnimeSearchResultDTO> results,
            Map<String, Integer> userGenrePreference) {

        int maxGenreCount = userGenrePreference.values().stream()
                .mapToInt(Integer::intValue).max().orElse(1);

        return results.stream()
                .map(anime -> {
                    double popularityScore = normalize(
                            anime.getPopularity() != null ? anime.getPopularity() : 0,
                            MAX_POPULARITY);

                    double scoreScore = normalize(
                            anime.getAverageScore() != null ? anime.getAverageScore() : 0,
                            MAX_SCORE);

                    // Trending boost: RELEASING anime được boost thêm
                    double trendingBoost = "RELEASING".equals(anime.getStatus()) ? 1.0 : 0.5;

                    // Personalized: tính theo genre preference
                    double personalizedScore = calcPersonalizedScore(
                            anime.getGenres(), userGenrePreference, maxGenreCount);

                    double finalScore =
                            wStructuredPopularity * popularityScore +
                                    wStructuredScore * scoreScore +
                                    wStructuredTrending * trendingBoost +
                                    wStructuredPersonalized * personalizedScore;

                    return anime.toBuilder().rankScore(finalScore).build();
                })
                .sorted(Comparator.comparingDouble(AnimeSearchResultDTO::getRankScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Rerank kết quả từ Embedding path (Vector Search).
     * cosineScore được truyền vào qua similarityScore trong DTO.
     */
    public List<AnimeSearchResultDTO> rerankSemantic(
            List<AnimeSearchResultDTO> results,
            Map<String, Integer> userGenrePreference) {

        int maxGenreCount = userGenrePreference.values().stream()
                .mapToInt(Integer::intValue).max().orElse(1);

        return results.stream()
                .map(anime -> {
                    double cosineScore = anime.getSimilarityScore() != null
                            ? anime.getSimilarityScore() : 0.0;

                    double popularityScore = normalize(
                            anime.getPopularity() != null ? anime.getPopularity() : 0,
                            MAX_POPULARITY);

                    double scoreScore = normalize(
                            anime.getAverageScore() != null ? anime.getAverageScore() : 0,
                            MAX_SCORE);

                    double personalizedScore = calcPersonalizedScore(
                            anime.getGenres(), userGenrePreference, maxGenreCount);

                    double finalScore =
                            wSemanticCosine * cosineScore +
                                    wSemanticPopularity * popularityScore +
                                    wSemanticScore * scoreScore +
                                    wSemanticPersonalized * personalizedScore;

                    return anime.toBuilder().rankScore(finalScore).build();
                })
                .sorted(Comparator.comparingDouble(AnimeSearchResultDTO::getRankScore).reversed())
                .collect(Collectors.toList());
    }

    // Normalize giá trị về [0, 1]
    private double normalize(double value, double max) {
        if (max == 0) return 0;
        return Math.min(value / max, 1.0);
    }

    // Tính personalized score dựa theo genre preference của user
    private double calcPersonalizedScore(
            List<String> animeGenres,
            Map<String, Integer> preference,
            int maxCount) {

        if (animeGenres == null || animeGenres.isEmpty()
                || preference == null || preference.isEmpty()) {
            return 0.0;
        }

        double totalScore = animeGenres.stream()
                .mapToInt(genre -> preference.getOrDefault(genre, 0))
                .sum();

        // Normalize: tổng genre count / (số genre * maxCount)
        return normalize(totalScore, (double) animeGenres.size() * maxCount);
    }
}