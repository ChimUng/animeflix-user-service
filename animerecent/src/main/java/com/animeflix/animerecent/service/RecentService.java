package com.animeflix.animerecent.service;

import com.animeflix.animerecent.model.RecentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecentService {
    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RecentService(WebClient.Builder builder, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<String> getRecentEpisodes() {
        String cacheKey = "recent";

        // Check cache
        return Mono.justOrEmpty(redisTemplate.opsForValue().get(cacheKey))
                .switchIfEmpty(
                        fetchRecentFromAnify()
                                .onErrorResume(e -> {
                                    System.err.println("Error fetching from Anify: " + e.getMessage());
                                    return fetchRecentFromConsumet();
                                })
                                .onErrorResume(e -> {
                                    System.err.println("Error fetching from Consumet: " + e.getMessage());
                                    return fetchRecentFromAnilist(1, 20);
                                })
                                .doOnNext(json -> {
                                    if (json != null && !json.isEmpty()) {
                                        redisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(1));
                                    }
                                })
                );
    }
    private Mono<String> fetchRecentFromAnify() {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.anify.tv")
                        .path("/recent")
                        .queryParam("type", "anime")
                        .queryParam("page", "1")
                        .queryParam("perPage", "20")
                        .queryParam("fields", "[id,title,status,format,currentEpisode,coverImage,totalEpisodes]")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::mapAnifyResponse);
    }

    private String mapAnifyResponse(String response) {
        try {
            List<Map<String, Object>> data = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() { });
            List<Map<String, Object>> mapped = data.stream().map(i -> {
                Integer current = (Integer) i.get("currentEpisode");
                @SuppressWarnings("unchecked")
                Map<String, Object> title = (Map<String, Object>) i.get("title");
                @SuppressWarnings("unchecked")
                Map<String, Object> cover = (Map<String, Object>) i.get("coverImage");

                return Map.of(
                        "id", i.get("id"),
                        "title", title,
                        "status", i.get("status"),
                        "format", i.get("format"),
                        "totalEpisodes", i.get("totalEpisodes"),
                        "currentEpisode", current,
                        "coverImage", cover,
                        "latestEpisode", String.valueOf(current + 1)
                );
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(mapped);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping Anify response", e);
        }
    }

    private Mono<String> fetchRecentFromConsumet() {
        return webClient.get()
                .uri("http://localhost:4000/anime/gogoanime/top-airing?page=1")
                .retrieve()
                .bodyToMono(String.class)
                .map(this::mapConsumetResponse);
    }

    private String mapConsumetResponse(String response) {
        try {
            Map<String, Object> data = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() { });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("results");
            List<Map<String, Object>> mapped = results.stream().map(item -> {
                @SuppressWarnings("unchecked")
                Integer epNum = (Integer) item.get("episodeNumber");
                String titleStr = (String) item.get("title");
                String image = (String) item.get("image");

                return Map.of(
                        "id", item.get("id"),
                        "latestEpisode", String.valueOf(epNum + 1),
                        "title", Map.of("romaji", titleStr, "english", titleStr),
                        "status", "Unknown",
                        "format", "TV",
                        "totalEpisodes", null,
                        "currentEpisode", epNum,
                        "coverImage", Map.of("medium", image, "large", image)
                );
            }).collect(Collectors.toList());
            return objectMapper.writeValueAsString(mapped);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping Consumet response", e);
        }
    }

    public Mono<String> fetchRecentFromAnilist(int page, int perPage) {
        try {
            String query = getRecentAnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .uri("https://graphql.anilist.co")
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing recent anime query: " + e.getMessage()));
        }
    }

//    private String mapAnilistResponse(RecentResponse response) {
//        try {
//            List<RecentResponse.Media> media = response.getData().getPage().getMedia();
//            if (media == null) {
//                return objectMapper.writeValueAsString(List.of());
//            }
//            List<Map<String, Object>> mapped = media.stream().map(m -> {
//                RecentResponse.NextAiringEpisode next = m.getNextAiringEpisode();
//                Integer nextEp = next != null ? next.getEpisode() : null;
//                String latest = nextEp != null ? nextEp.toString() : "1";
//                Integer current = nextEp != null ? nextEp - 1 : null;
//                RecentResponse.Title titleObj = m.getTitle();
//                RecentResponse.CoverImage coverObj = m.getCoverImage();
//
//                Map<String, String> titleMap = Map.of(
//                        "romaji", titleObj.getRomaji(),
//                        "english", titleObj.getEnglish()
//                );
//                Map<String, String> coverMap = Map.of(
//                        "medium", coverObj.getMedium(),
//                        "large", coverObj.getLarge()
//                );
//
//                return Map.of(
//                        "id", String.valueOf(m.getId()),
//                        "title", titleMap,
//                        "status", m.getStatus(),
//                        "format", m.getFormat(),
//                        "totalEpisodes", m.getEpisodes(),
//                        "currentEpisode", current,
//                        "coverImage", coverMap,
//                        "latestEpisode", latest
//                );
//            }).collect(Collectors.toList());
//            return objectMapper.writeValueAsString(mapped);
//        } catch (Exception e) {
//            throw new RuntimeException("Error mapping Anilist response", e);
//        }
//    }

    //testinggggggggggggggggggggggg

//    public Mono<String> getRecentAnime(int page, int perPage) {
//        try {
//            String query = getRecentAnimeQuery();
//            Map<String, Object> body = Map.of(
//                    "query", query,
//                    "variables", Map.of("page", page, "perPage", perPage)
//            );
//
//            return webClient.post()
//                    .uri("https://graphql.anilist.co")
//                    .header("Content-Type", "application/json")
//                    .bodyValue(body)
//                    .retrieve()
//                    .bodyToMono(String.class);
//        } catch (Exception e) {
//            return Mono.error(new RuntimeException("Error preparing recent anime query: " + e.getMessage()));
//        }
//    }


    private String getRecentAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("recent-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }
}