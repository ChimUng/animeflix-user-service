package com.animeflix.animecatalogservice.service;

import com.animeflix.animecatalogservice.DTO.AnimeDetailResponse;
import com.animeflix.animecatalogservice.DTO.AnimeResponse;
import com.animeflix.animecatalogservice.Entity.Anime;
import com.animeflix.animecatalogservice.Entity.AnimeSchedule;
import com.animeflix.animecatalogservice.Repository.ScheduleRepository;
import com.animeflix.animecatalogservice.exception.*;
import com.animeflix.animecatalogservice.mapper.AnimeMapper;
import com.animeflix.animecatalogservice.Repository.AnimeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeService {

    private final AnimeRepository animeRepository;
    private final ScheduleRepository scheduleRepository;
    private final AnimeMapper animeMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final AnimeSyncService animeSyncService;

    // Helper: Logic Cache-Aside Generic
    private <T> Mono<T> getFromCacheOrDb(String key, Mono<T> dbFallback, TypeReference<T> typeRef) {
        return Mono.justOrEmpty(redisTemplate.opsForValue().get(key))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, typeRef));
                    } catch (Exception e) {
                        log.warn("Cache parse error for key: {}, deleting...", key);
                        redisTemplate.delete(key);
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(dbFallback.doOnNext(data -> {
                    try {
                        String json = objectMapper.writeValueAsString(data);
                        redisTemplate.opsForValue().set(key, json, Duration.ofHours(6));
                    } catch (Exception e) {
                        log.warn("Không thể cache vào Redis: {}", e.getMessage());
                    }
                }));
    }

    // 1. Get Anime Detail (CẬP NHẬT LOGIC LAZY LOAD)
    public Mono<AnimeDetailResponse> getAnimeInfo(String id) {
        String key = "id:" + id;
        return getFromCacheOrDb(key,
                Mono.defer(() -> { // Sử dụng defer để trì hoãn thực thi cho đến khi subscribe

                    // Bước 1: Tìm trong DB
                    return Mono.justOrEmpty(animeRepository.findById(id))
                            .flatMap(anime -> {
                                // Bước 2: Kiểm tra xem data có đủ Detail không?
                                // Ví dụ: check xem list Character có null không
                                boolean isMissingDetail = anime.getCharacters() == null ||
                                        anime.getRelations() == null;

                                if (isMissingDetail) {
                                    log.info("Anime {} found but missing details. Triggering Sync...", id);
                                    // Gọi Sync Service để lấy full data từ Anilist
                                    return animeSyncService.fetchAndSaveAnimeDetail(id);
                                } else {
                                    return Mono.just(anime);
                                }
                            })
                            // Bước 3: Nếu DB chưa có luôn -> Cũng gọi Sync
                            .switchIfEmpty(Mono.defer(() -> {
                                log.info("Anime {} not found in DB. Triggering Sync...", id);
                                return animeSyncService.fetchAndSaveAnimeDetail(id);
                            }))
                            // Bước 4: Map sang DTO
                            .map(animeMapper::toDetailResponse);
                }),
                new TypeReference<>() {}
        );
    }

    // 2. Get Popular
    public Mono<List<AnimeResponse>> getPopularAnime(int page, int perPage) {
        String key = "popular:" + page + ":" + perPage;
        return getFromCacheOrDb(key,
                Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
                    return animeRepository.findAll(pageable).stream()
                            .map(animeMapper::toResponse)
                            .collect(Collectors.toList());
                }),
                new TypeReference<>() {}
        );
    }

    // 3. Get Trending (Sắp xếp theo score hoặc logic riêng)
    public Mono<List<AnimeResponse>> getTrendingAnime(int page, int perPage) {
        String key = "trending:" + page + ":" + perPage;
        return getFromCacheOrDb(key,
                Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("averageScore").descending());
                    return animeRepository.findAll(pageable).stream()
                            .map(animeMapper::toResponse)
                            .collect(Collectors.toList());
                }),
                new TypeReference<>() {}
        );
    }

    // 4. Get Movies (Format = MOVIE)
    public Mono<List<AnimeResponse>> getPopularMovie(int page, int perPage) {
        String key = "movie:" + page + ":" + perPage;
        return getFromCacheOrDb(key,
                Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
                    return animeRepository.findByFormat("MOVIE", pageable).stream()
                            .map(animeMapper::toResponse)
                            .collect(Collectors.toList());
                }),
                new TypeReference<>() {}
        );
    }

    // 5. Get Season Anime (Ví dụ: WINTER 2024)
    public Mono<List<AnimeResponse>> getSeasonAnime(int page, int perPage) {
        // Logic tính season hiện tại
        Map<String, Object> current = getCurrentSeasonAndYear();
        String season = (String) current.get("season");
        Integer year = (Integer) current.get("year");

        String key = "season:" + season + ":" + year + ":" + page;

        return getFromCacheOrDb(key,
                Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
                    return animeRepository.findBySeasonAndSeasonYear(season, year, pageable).stream()
                            .map(animeMapper::toResponse)
                            .collect(Collectors.toList());
                }),
                new TypeReference<>() {}
        );
    }

    // 6. Get top 100 Anime

    public Mono<List<AnimeResponse>> getTop100Anime(int page, int perPage) {
        String key = "top100:" + page;
        return getFromCacheOrDb(key, Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("averageScore").descending());
            return animeRepository.findAll(pageable).getContent().stream()
                    .map(animeMapper::toResponse)
                    .limit(100)
                    .collect(Collectors.toList());
        }), new TypeReference<>() {});
    }

    // 7. Get current SeasonAnime
    public Mono<List<AnimeResponse>> getCurrentSeasonAnime(int page, int perPage) {
        Map<String, Object> current = getCurrentSeasonAndYear();
        String season = (String) current.get("season");
        int year = (Integer) current.get("year");
        String key = "currentseason:" + season + ":" + year + ":" + page;

        return getFromCacheOrDb(key, Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
            return animeRepository.findBySeasonAndSeasonYear(season, year, pageable)
                    .stream()
                    .map(animeMapper::toResponse)
                    .collect(Collectors.toList());
        }), new TypeReference<>() {});
    }

    // 8. Get next SeasonAnime
    public Mono<List<AnimeResponse>> getNextSeasonAnime(int page, int perPage) {
        Map<String, Object> next = getNextSeasonAndYear();
        String season = (String) next.get("season");
        int year = (Integer) next.get("year");
        String key = "nextseason:" + season + ":" + year + ":" + page;

        return getFromCacheOrDb(key, Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
            return animeRepository.findBySeasonAndSeasonYear(season, year, pageable)
                    .stream()
                    .map(animeMapper::toResponse)
                    .collect(Collectors.toList());
        }), new TypeReference<>() {});
    }

    // 9. Get Popular Movie Anime
    public Mono<List<AnimeResponse>> getPopularMovies(int page, int perPage) {
        String key = "movies:" + page;
        return getFromCacheOrDb(key, Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("popularity").descending());
            return animeRepository.findByFormat("MOVIE", pageable)
                    .stream()
                    .map(animeMapper::toResponse)
                    .collect(Collectors.toList());
        }), new TypeReference<>() {});
    }

    // 10. Get Schedule (Tìm các anime có tập mới trong 7 ngày tới)
    public Mono<Map<String, Object>> getAnimeSchedule(long airingAtGreater) {
        String key = "schedule:week";

        return getFromCacheOrDb(key,
                Mono.fromCallable(() -> {
                    long start = airingAtGreater > 0 ? airingAtGreater : Instant.now().getEpochSecond();
                    long end = Instant.now().plus(Duration.ofDays(7)).getEpochSecond();

                    // Query schedules từ MongoDB
                    List<AnimeSchedule> schedules = scheduleRepository
                            .findByAiringAtBetweenOrderByAiringAtAsc(start, end);

                    // Transform sang format response
                    List<Map<String, Object>> animes = schedules.stream()
                            .map(schedule -> {
                                Map<String, Object> anime = new HashMap<>();
                                anime.put("id", schedule.getAnimeId());

                                // Handle null title
                                if (schedule.getTitle() != null) {
                                    anime.put("title", Map.of(
                                            "romaji", schedule.getTitle().getRomaji() != null ? schedule.getTitle().getRomaji() : "",
                                            "english", schedule.getTitle().getEnglish(),
                                            "native", schedule.getTitle().getNativeTitle() != null ? schedule.getTitle().getNativeTitle() : ""
                                    ));
                                }

                                anime.put("episode", schedule.getEpisode());
                                anime.put("airingAt", schedule.getAiringAt());
                                anime.put("airingTime", schedule.getAiringTime());
                                anime.put("coverImage", schedule.getCoverImage());
                                anime.put("siteUrl", "https://anilist.co/anime/" + schedule.getAnimeId());
                                anime.put("format", schedule.getFormat());
                                anime.put("status", schedule.getStatus());
                                anime.put("episodes", schedule.getEpisodes());
                                anime.put("bannerImage", schedule.getBannerImage());
                                anime.put("day", schedule.getDay());

                                return anime;
                            })
                            .collect(Collectors.toList());

                    // Count by day using aggregation
                    List<ScheduleRepository.DayCountProjection> dayCounts =
                            scheduleRepository.countByDay(start, end);

                    // Map to count map
                    Map<String, Long> countsMap = dayCounts.stream()
                            .collect(Collectors.toMap(
                                    ScheduleRepository.DayCountProjection::getDay,
                                    ScheduleRepository.DayCountProjection::getCount
                            ));

                    // Create days list
                    List<Map<String, Object>> days = Arrays.asList(
                                    "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư",
                                    "Thứ năm", "Thứ sáu", "Thứ bảy"
                            ).stream()
                            .map(day -> Map.of(
                                    "day", (Object) day,
                                    "count", (Object) countsMap.getOrDefault(day, 0L)
                            ))
                            .collect(Collectors.toList());

                    return Map.of(
                            "days", days,
                            "animes", animes
                    );
                }),
                new TypeReference<Map<String, Object>>() {}
        );
    }

    // 11. Search Nâng Cao (Dùng MongoTemplate cho linh động)
    public Mono<List<AnimeResponse>> searchAnime(String search, Integer year, String season, String format, List<String> genres, String sortBy, int page, int perPage) {
        return Mono.fromCallable(() -> animeSyncService.loadGraphqlQuery("advanced-search.graphql"))
                .flatMap(query -> {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("page", page);
                    variables.put("perPage", perPage);
                    variables.put("type", "ANIME");  // Fixed
                    if (search != null && !search.isEmpty()) {
                        variables.put("search", search);
                    }
                    if (year != null) {
                        variables.put("seasonYear", year);
                    }
                    if (season != null && !season.isEmpty()) {
                        variables.put("season", season.toUpperCase());
                    }
                    if (format != null && !format.isEmpty()) {
                        variables.put("format", List.of(format.toUpperCase()));
                    }
                    if (genres != null && !genres.isEmpty()) {
                        variables.put("genres", genres);
                    }
                    if (sortBy != null && !sortBy.isEmpty()) {
                        String sort = sortBy.toUpperCase();
                        if (search != null && !search.isEmpty()) {
                            sort = "SEARCH_MATCH";
                        }
                        variables.put("sort", List.of(sort));
                    }

                    return webClient.post()
                            .bodyValue(Map.of("query", query, "variables", variables))
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .map(response -> {
                                // Kiểm tra error từ Anilist
                                if (response.has("errors")) {
                                    throw new BusinessException("ANILIST_ERROR", "Anilist API error: " + response.get("errors").toString(), "Lỗi từ nguồn dữ liệu Anilist");
                                }
                                // Parse media list (dùng .elements() để iterate JsonNode array)
                                JsonNode mediaList = response.path("data").path("Page").path("media");
                                List<AnimeResponse> results = new ArrayList<>();
                                if (mediaList.isArray()) {
                                    mediaList.elements().forEachRemaining(node -> {
                                        try {
                                            Anime anime = objectMapper.treeToValue(node, Anime.class);
                                            results.add(animeMapper.toResponse(anime));
                                        } catch (Exception e) {
                                            log.error("Parse error for media node: {}", node, e);
                                        }
                                    });
                                }
                                return results;
                            });
                })
                .onErrorResume(ex -> Mono.error(new BusinessException("SEARCH_ERROR", "Failed to search anime: " + ex.getMessage(), "Lỗi tìm kiếm anime")));
    }

    // logic caculator day/month/year
    private Map<String, Object> getCurrentSeasonAndYear() {
        int month = java.time.LocalDate.now().getMonthValue();
        int year = java.time.LocalDate.now().getYear();
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
}
