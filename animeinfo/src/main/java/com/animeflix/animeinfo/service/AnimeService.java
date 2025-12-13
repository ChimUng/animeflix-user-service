package com.animeflix.animeinfo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnimeService {
    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public AnimeService(WebClient.Builder builder, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.webClient = builder
                .baseUrl("https://graphql.anilist.co")
                .build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<String> getAnimeInfo(String id) {
        try {
            String query = getAnimeInfoQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("id", Integer.parseInt(id))
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing anime info query: " + e.getMessage()));
        }
    }

    public Mono<String> getFavouriteAnime(int page, int perPage) {
        try {
            String query = getFavouriteAnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing favourite anime query: " + e.getMessage()));
        }
    }

    public Mono<String> getAnimeSchedule(int page, int perPage, long airingAtGreater) {
        String cacheKey = "schedule:week";

        // Check cache
        return Mono.justOrEmpty(redisTemplate.opsForValue().get(cacheKey))
                .switchIfEmpty(
                        fetchAnimeSchedule(page, perPage, airingAtGreater)
                                .flatMap(result -> {
                                    try {
                                        String resultJson = objectMapper.writeValueAsString(result);
                                        redisTemplate.opsForValue().set(cacheKey, resultJson, Duration.ofHours(24));
                                        return Mono.just(resultJson);
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("Error serializing result to JSON", e));
                                    }
                                })
                );
    }

    private Mono<Map<String, Object>> fetchAnimeSchedule(int page, int perPage, long airingAtGreater) {
        long start = airingAtGreater > 0 ? airingAtGreater : Instant.now().getEpochSecond();
        long end = Instant.now().plus(Duration.ofDays(7)).getEpochSecond();

        return Flux.generate(
                        () -> page,
                        (currentPage, sink) -> {
                            try {
                                String query = getScheduleAnimeQuery();
                                Map<String, Object> body = Map.of(
                                        "query", query,
                                        "variables", Map.of("page", currentPage, "perPage", perPage, "from", start, "to", end)
                                );

                                webClient.post()
                                        .header("Content-Type", "application/json")
                                        .bodyValue(body)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .map(response -> {
                                            try {
                                                return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                                            } catch (Exception e) {
                                                throw new RuntimeException("Error parsing response JSON", e);
                                            }
                                        })
                                        .subscribe(data -> {
                                            Map<String, Object> pageData = (Map<String, Object>) ((Map<String, Object>) data.get("data")).get("Page");
                                            List<Map<String, Object>> schedules = (List<Map<String, Object>>) pageData.get("airingSchedules");
                                            boolean hasNextPage = (Boolean) ((Map<String, Object>) pageData.get("pageInfo")).get("hasNextPage");

                                            sink.next(Map.of(
                                                    "schedules", schedules,
                                                    "hasNextPage", hasNextPage
                                            ));

                                            if (!hasNextPage) {
                                                sink.complete();
                                            }
                                        }, sink::error);

                                return currentPage + 1;
                            } catch (Exception e) {
                                sink.error(new RuntimeException("Error fetching schedule query", e));
                                return currentPage;
                            }
                        }
                )
                .cast(Map.class)
                .takeWhile(data -> (Boolean) data.get("hasNextPage") || !((List<?>) data.get("schedules")).isEmpty())
                .flatMap(data -> Mono.just((List<Map<String, Object>>) data.get("schedules")))
                .collectList()
                .map(allSchedulesList -> allSchedulesList.stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList()))
                .map(allSchedules -> {
                    // Map to Anime format
                    List<Map<String, Object>> animes = allSchedules.stream().map(item -> {
                        Map<String, Object> media = (Map<String, Object>) item.get("media");
                        Map<String, Object> title = (Map<String, Object>) media.get("title");
                        Map<String, Object> coverImage = (Map<String, Object>) media.get("coverImage");

                        String day = Instant.ofEpochSecond((Integer) item.get("airingAt"))
                                .atZone(ZoneId.systemDefault())
                                .getDayOfWeek()
                                .getDisplayName(TextStyle.FULL, new Locale("vi", "VN"));

                        return Map.ofEntries(
                                Map.entry("id", media.get("id")),
                                Map.entry("title", Map.of(
                                        "romaji", title.get("romaji"),
                                        "english", title.get("english"),
                                        "native", title.get("native")
                                )),
                                Map.entry("episode", item.get("episode")),
                                Map.entry("airingAt", item.get("airingAt")),
                                Map.entry("airingTime", Instant.ofEpochSecond((Integer) item.get("airingAt"))
                                        .atZone(ZoneId.systemDefault())
                                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))),
                                Map.entry("coverImage", coverImage.get("large")),
                                Map.entry("siteUrl", media.get("siteUrl")),
                                Map.entry("format", media.get("format")),
                                Map.entry("status", media.get("status")),
                                Map.entry("episodes", media.get("episodes") != null ? media.get("episodes") : null),
                                Map.entry("bannerImage", media.get("bannerImage")),
                                Map.entry("day", day)
                        );
                    }).collect(Collectors.toList());

                    // Group by day and count
                    Map<String, Long> counts = animes.stream()
                            .collect(Collectors.groupingBy(
                                    anime -> (String) anime.get("day"),
                                    Collectors.counting()
                            ));

                    List<Map<String, Object>> days = Arrays.asList(
                            "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"
                    ).stream().map(day -> Map.of(
                            "day", (Object) day,
                            "count", (Object) counts.getOrDefault(day, 0L)
                    )).collect(Collectors.toList());

                    // Prepare result
                    return Map.of(
                            "days", days,
                            "animes", animes
                    );
                });
    }

    public Mono<String> getSeasonAnime(int page, int perPage) {
        try {
            String query = getSeasonAnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing season anime query: " + e.getMessage()));
        }
    }

    public Mono<String> getPopularMovie(int page, int perPage) {
        try {
            String query = getPopularMovieQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage, "type", "MOVIE")
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing popular movie query: " + e.getMessage()));
        }
    }

    public Mono<String> getPopularThisSeason(int page, int perPage) {
        try {
            Map<String, Object> seasonInfo = getCurrentSeasonAndYear();
            String query = getPopularThisSeasonQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of(
                            "page", page,
                            "perPage", perPage,
                            "season", seasonInfo.get("season"),
                            "seasonYear", seasonInfo.get("year")
                    )
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing popular this season query: " + e.getMessage()));
        }
    }

    public Mono<String> getPopularNextSeason(int page, int perPage) {
        try {
            Map<String, Object> seasonInfo = getNextSeasonAndYear();
            String query = getPopularNextSeasonQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of(
                            "page", page,
                            "perPage", perPage,
                            "season", seasonInfo.get("season"),
                            "seasonYear", seasonInfo.get("year")
                    )
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing popular next season query: " + e.getMessage()));
        }
    }

    public Mono<String> getTop100Anime(int page, int perPage) {
        try {
            String query = getTop100AnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing top 100 anime query: " + e.getMessage()));
        }
    }

    public Mono<String> getPopularAnime(int page, int perPage) {
        try {
            String query = getPopularAnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing popular anime query: " + e.getMessage()));
        }
    }

    public Mono<String> getTrendingAnime(int page, int perPage) {
        try {
            String query = getTrendingAnimeQuery();
            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("page", page, "perPage", perPage)
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing trending anime query: " + e.getMessage()));
        }
    }

    public Mono<String> searchAnime(String searchValue, Integer year, String season, String format, List<Map<String, String>> genres, String sortBy, int page) {
        try {
            String query = getSearchAnimeQuery();
            Map<String, Object> variables = new HashMap<>();
            variables.put("page", page);
            variables.put("perPage", 10); // Default perPage, can be adjusted
            if (searchValue != null && !searchValue.isEmpty()) {
                variables.put("search", searchValue);
                variables.put("sort", List.of("SEARCH_MATCH"));
            }
            if (year != null) {
                variables.put("seasonYear", year);
            }
            if (season != null && !season.isEmpty()) {
                variables.put("season", season);
            }
            if (format != null && !format.isEmpty()) {
                variables.put("format", List.of(format));
            }
            if (sortBy != null && !sortBy.isEmpty() && (searchValue == null || searchValue.isEmpty())) {
                variables.put("sort", List.of(sortBy));
            }
            if (genres != null && !genres.isEmpty()) {
                Map<String, List<String>> genreMap = genres.stream()
                        .collect(Collectors.groupingBy(
                                genre -> genre.get("type"),
                                Collectors.mapping(genre -> genre.get("value"), Collectors.toList())
                        ));
                variables.putAll(genreMap);
            }

            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", variables
            );

            return webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error preparing search anime query: " + e.getMessage()));
        }
    }

    // logic caculator day/month/year
    private Map<String, Object> getCurrentSeasonAndYear() {
        int month = Instant.now().atZone(ZoneId.systemDefault()).getMonthValue();
        int year = Instant.now().atZone(ZoneId.systemDefault()).getYear();
        String season;
        if (month <= 3) season = "WINTER";
        else if (month <= 6) season = "SPRING";
        else if (month <= 9) season = "SUMMER";
        else season = "FALL";
        return Map.of("season", season, "year", year);
    }

    private Map<String, Object> getNextSeasonAndYear() {
        String[] seasons = {"WINTER", "SPRING", "SUMMER", "FALL"};
        int month = Instant.now().atZone(ZoneId.systemDefault()).getMonthValue();
        int year = Instant.now().atZone(ZoneId.systemDefault()).getYear();
        int currentSeasonIndex = month <= 3 ? 0 : month <= 6 ? 1 : month <= 9 ? 2 : 3;
        int nextSeasonIndex = (currentSeasonIndex + 1) % 4;
        int nextYear = nextSeasonIndex == 0 ? year + 1 : year;
        return Map.of("season", seasons[nextSeasonIndex], "year", nextYear);
    }
    /////////////////////////////////////////////////

    private String getAnimeInfoQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("anime-info.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getFavouriteAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("favourite-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getScheduleAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("schedule-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getSeasonAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("seasonal-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getPopularMovieQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("popular-movie.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getPopularThisSeasonQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("popular-this-season.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getPopularNextSeasonQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("popular-this-season.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getTop100AnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("top-100-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getPopularAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("popular-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getTrendingAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("trending-anime.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

    private String getSearchAnimeQuery() throws Exception {
        ClassPathResource resource = new ClassPathResource("advanced-search.graphql");
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }
}