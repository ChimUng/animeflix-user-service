package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.dto.response.AnimeSearchResultDTO;
import com.animeflix.aisearchservice.dto.response.ParsedQueryDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AniListApiService {

    private final WebClient aniListWebClient;

    private static final String SEARCH_QUERY = """
            query ($page: Int, $perPage: Int, $type: MediaType, $genres: [String],
                   $tags: [String], $format: MediaFormat, $status: MediaStatus,
                   $season: MediaSeason, $seasonYear: Int, $sort: [MediaSort]) {
                Page(page: $page, perPage: $perPage) {
                    pageInfo { total hasNextPage }
                    media(type: $type, genre_in: $genres, tag_in: $tags,
                          format: $format, status: $status,
                          season: $season, seasonYear: $seasonYear, sort: $sort) {
                        id
                        title { romaji english userPreferred }
                        coverImage { large extraLarge }
                        bannerImage
                        genres
                        averageScore
                        popularity
                        status
                        format
                        season
                        seasonYear
                    }
                }
            }
            """;

    /**
     * Gọi AniList API với filter đã parse từ Gemini.
     * Chỉ gọi khi fallbackToEmbedding = false.
     */
    public Mono<List<AnimeSearchResultDTO>> search(ParsedQueryDTO parsed, int page, int perPage) {
        Map<String, Object> variables = buildVariables(parsed, page, perPage);

        log.info("📡 Calling AniList API with: genres={}, status={}, format={}",
                parsed.getGenres(), parsed.getStatus(), parsed.getFormat());

        return aniListWebClient.post()
                .bodyValue(Map.of("query", SEARCH_QUERY, "variables", variables))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    if (response.has("errors")) {
                        log.error("AniList API error: {}", response.get("errors"));
                        return new ArrayList<AnimeSearchResultDTO>();
                    }

                    JsonNode mediaList = response
                            .path("data")
                            .path("Page")
                            .path("media");

                    List<AnimeSearchResultDTO> results = new ArrayList<>();
                    if (mediaList.isArray()) {
                        mediaList.elements().forEachRemaining(node ->
                                results.add(mapNodeToDTO(node))
                        );
                    }

                    log.info("✅ AniList returned {} results", results.size());
                    return results;
                })
                .onErrorReturn(new ArrayList<>());
    }

    private Map<String, Object> buildVariables(ParsedQueryDTO parsed, int page, int perPage) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", "ANIME");
        variables.put("page", page);
        variables.put("perPage", perPage);

        // Chỉ thêm filter khi có giá trị
        if (parsed.getGenres() != null && !parsed.getGenres().isEmpty()) {
            variables.put("genres", parsed.getGenres());
        }
        if (parsed.getTags() != null && !parsed.getTags().isEmpty()) {
            variables.put("tags", parsed.getTags());
        }
        if (parsed.getFormat() != null) {
            variables.put("format", parsed.getFormat());
        }
        if (parsed.getStatus() != null) {
            variables.put("status", parsed.getStatus());
        }
        if (parsed.getSeason() != null) {
            variables.put("season", parsed.getSeason());
        }
        if (parsed.getSeasonYear() != null) {
            variables.put("seasonYear", parsed.getSeasonYear());
        }

        // Default sort nếu Gemini không trả sort
        List<String> sort = (parsed.getSort() != null && !parsed.getSort().isEmpty())
                ? parsed.getSort()
                : List.of("POPULARITY_DESC");
        variables.put("sort", sort);

        return variables;
    }

    private AnimeSearchResultDTO mapNodeToDTO(JsonNode node) {
        return AnimeSearchResultDTO.builder()
                .id(node.path("id").asText())
                .titleRomaji(node.path("title").path("romaji").asText(null))
                .titleEnglish(node.path("title").path("english").asText(null))
                .titleUserPreferred(node.path("title").path("userPreferred").asText(null))
                .coverImage(node.path("coverImage").path("large").asText(null))
                .bannerImage(node.path("bannerImage").asText(null))
                .averageScore(node.path("averageScore").asInt(0))
                .popularity(node.path("popularity").asInt(0))
                .status(node.path("status").asText(null))
                .format(node.path("format").asText(null))
                .season(node.path("season").asText(null))
                .seasonYear(node.path("seasonYear").asInt(0))
                .build();
    }
}