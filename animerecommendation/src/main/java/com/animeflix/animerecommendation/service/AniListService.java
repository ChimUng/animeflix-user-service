package com.animeflix.animerecommendation.service;

import com.animeflix.animerecommendation.model.AnimeMeta;
import com.animeflix.animerecommendation.model.RecentEpisode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AniListService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AniListService(@Qualifier("webClient") WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public Mono<AnimeMeta> fetchAnimeMeta(String animeId) {
        if (animeId == null || animeId.trim().isEmpty() || !animeId.matches("\\d+")) {  // Check numeric
            System.out.println(">>> [WARN] Skip invalid aniId: " + animeId);
            return Mono.justOrEmpty(null);  // Trả empty Mono (không error, filter sau sẽ loại)
        }

        String query = """
        query ($id: Int) {
          Media(id: $id, type: ANIME) {
            id
            title {
              romaji
              english
            }
            genres
            tags {
              name
            }
          }
        }
        """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", Integer.valueOf(animeId));  // Giờ an toàn

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("variables", variables);

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Parse logic giữ nguyên
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    if (data == null || !data.containsKey("Media")) {
                        System.out.println(">>> [WARN] No media data for aniId: " + animeId);
                        return null;
                    }
                    Map<String, Object> mediaMap = (Map<String, Object>) data.get("Media");
                    return objectMapper.convertValue(mediaMap, AnimeMeta.class);
                })
                .onErrorReturn(new AnimeMeta());  // Fallback: Empty AnimeMeta (không null, genres=empty)
    }

    public Mono<List<RecentEpisode>> fetchRecentFromAnilist() {
        String query = """
            query {
              Page(page: 1, perPage: 20) {
                media(sort: TRENDING_DESC, type: ANIME, status: RELEASING) {
                  id
                  title {
                    romaji
                    english
                  }
                  coverImage {
                    medium
                    large
                  }
                  episodes
                  nextAiringEpisode {
                    episode
                    airingAt
                  }
                  status
                  format
                  genres
                }
              }
            }
            """;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);

        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseRecentAnime)
                .onErrorReturn(List.of());
    }

    private List<RecentEpisode> parseRecentAnime(String response) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            Map<String, Object> page = (Map<String, Object>) data.get("Page");
            List<Map<String, Object>> mediaList = (List<Map<String, Object>>) page.get("media");
            return mediaList.stream()
                    .map(this::mapToRecentEpisode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing recent anime response", e);
        }
    }

    private RecentEpisode mapToRecentEpisode(Map<String, Object> media) {
        RecentEpisode episode = new RecentEpisode();
        episode.setId(((Integer) media.get("id")).toString());

        Map<String, String> titleMap = (Map<String, String>) media.get("title");
        RecentEpisode.Title title = new RecentEpisode.Title();
        title.setRomaji(titleMap.get("romaji"));
        title.setEnglish(titleMap.get("english"));
        episode.setTitle(title);

        episode.setGenres((List<String>) media.get("genres"));

        Map<String, String> coverMap = (Map<String, String>) media.get("coverImage");
        RecentEpisode.CoverImage cover = new RecentEpisode.CoverImage();
        cover.setMedium(coverMap.get("medium"));
        cover.setLarge(coverMap.get("large"));
        episode.setCoverImage(cover);

        Map<String, Object> next = (Map<String, Object>) media.get("nextAiringEpisode");
        if (next != null) {
            Integer epNum = (Integer) next.get("episode");
            episode.setCurrentEpisode(epNum != null ? epNum - 1 : null);
            episode.setLatestEpisode(epNum != null ? epNum.toString() : "1");
        } else {
            episode.setCurrentEpisode(null);
            episode.setLatestEpisode("1");
        }

        episode.setTotalEpisodes((Integer) media.get("episodes"));
        episode.setStatus((String) media.get("status"));
        episode.setFormat((String) media.get("format"));

        return episode;
    }
}