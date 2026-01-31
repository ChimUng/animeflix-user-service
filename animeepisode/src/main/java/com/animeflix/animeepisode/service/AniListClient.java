package com.animeflix.animeepisode.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * AniList GraphQL Client
 * Fetch anime metadata để support AnimePahe search
 */
@Component
@Slf4j
public class AniListClient {

    private final WebClient webClient;

    private static final String ANILIST_GRAPHQL_URL = "https://graphql.anilist.co";

    public AniListClient() {
        this.webClient = WebClient.builder()
                .baseUrl(ANILIST_GRAPHQL_URL)
                .build();
    }

    /**
     * Fetch anime info từ AniList GraphQL
     *
     * @param anilistId AniList ID
     * @return AnimeInfo với title, year, format
     */
    public Mono<AnimeInfo> getAnimeInfo(String anilistId) {
        String query = """
            query ($id: Int) {
              Media(id: $id) {
                title {
                  romaji
                  english
                  native
                }
                startDate {
                  year
                }
                format
              }
            }
            """;

        Map<String, Object> variables = Map.of("id", Integer.parseInt(anilistId));
        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", variables
        );

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AniListResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> {
                    if (response.getData() == null || response.getData().getMedia() == null) {
                        log.warn("⚠️ No AniList data for ID: {}", anilistId);
                        return null;
                    }

                    Media media = response.getData().getMedia();

                    // Priority: english > romaji > native
                    String title = media.getTitle().getEnglish() != null
                            ? media.getTitle().getEnglish()
                            : (media.getTitle().getRomaji() != null
                            ? media.getTitle().getRomaji()
                            : media.getTitle().getNativeTitle());

                    Integer year = media.getStartDate() != null
                            ? media.getStartDate().getYear()
                            : null;

                    log.info("📺 AniList info: \"{}\" ({}, {})",
                            title, year, media.getFormat());

                    return AnimeInfo.builder()
                            .title(title)
                            .year(year)
                            .format(media.getFormat())
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("❌ Error fetching AniList info for {}: {}",
                            anilistId, e.getMessage());
                    return Mono.empty();
                });
    }

    // ========================================
    // DTOs - Internal classes
    // ========================================

    @Data
    private static class AniListResponse {
        @JsonProperty("data")
        private DataWrapper data;
    }

    @Data
    private static class DataWrapper {
        @JsonProperty("Media")
        private Media media;
    }

    @Data
    private static class Media {
        @JsonProperty("title")
        private Title title;

        @JsonProperty("startDate")
        private StartDate startDate;

        @JsonProperty("format")
        private String format;
    }

    @Data
    private static class Title {
        @JsonProperty("romaji")
        private String romaji;

        @JsonProperty("english")
        private String english;

        @JsonProperty("native")
        private String nativeTitle;
    }

    @Data
    private static class StartDate {
        @JsonProperty("year")
        private Integer year;
    }

    /**
     * Public DTO cho anime info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnimeInfo {
        private String title;   // English hoặc Romaji
        private Integer year;   // Năm phát hành
        private String format;  // TV, MOVIE, OVA, ONA, SPECIAL
    }
}