package com.animeflix.animecatalogservice.service;

import com.animeflix.animecatalogservice.Entity.Anime;
import com.animeflix.animecatalogservice.Entity.AnimeSchedule;
import com.animeflix.animecatalogservice.Repository.AnimeRepository;
import com.animeflix.animecatalogservice.Repository.ScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimeSyncService {

    private final WebClient webClient;
    private final AnimeRepository animeRepository;
    private final ScheduleRepository scheduleRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        log.info("=== Ứng dụng đã sẵn sàng - Bắt đầu sync dữ liệu lần đầu ===");
        syncAllData();
    }

    @Scheduled(cron = "0 0 */6 * * ?")
    public void syncAllData() {
        log.info("=== START SYNC DATA ===");
        syncFromGraphql("favourite-anime.graphql", Map.of("page", 1, "perPage", 20));
        syncFromGraphql("popular-anime.graphql", Map.of("page", 1, "perPage", 20));
//        syncFromGraphql("popular-movie.graphql", Map.of("page", 1, "perPage", 20));
//        Map<String, Object> current = getCurrentSeasonAndYear();
//        syncFromGraphql("popular-this-season.graphql", Map.of("page", 1, "perPage", 20, "season", current.get("season"), "seasonYear", current.get("year")));
//        Map<String, Object> next = getNextSeasonAndYear();
//        syncFromGraphql("popular-this-season.graphql", Map.of("page", 1, "perPage", 20, "season", next.get("season"), "seasonYear", next.get("year")));
//        syncFromGraphql("seasonal-anime.graphql", Map.of("page", 1, "perPage", 20));
//        syncFromGraphql("top-100-anime.graphql", Map.of("page", 1, "perPage", 20));
//        syncFromGraphql("trending-anime.graphql", Map.of("page", 1, "perPage", 20));
        syncSchedules();
        log.info("=== END SYNC DATA ===");
    }

    public void syncFromGraphql(String filename, Map<String, Object> variables) {
        try {
            String query = loadGraphqlQuery(filename);

            webClient.post()
                    .bodyValue(Map.of("query", query, "variables", variables))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .subscribe(response -> {
                        JsonNode mediaList = response.path("data").path("Page").path("media");
                        if (mediaList.isArray()) {
                            List<Anime> newAnimes = new ArrayList<>();
                            List<String> newIds = new ArrayList<>();

                            for (JsonNode node : mediaList) {
                                Anime anime = mapJsonToEntity(node);
                                if (anime != null) {
                                    newAnimes.add(anime);
                                    newIds.add(anime.getId());
                                }
                            }

                            List<Anime> existingAnimes = animeRepository.findAllById(newIds);
                            Map<String, Anime> existingMap = existingAnimes.stream()
                                    .collect(Collectors.toMap(Anime::getId, Function.identity()));

                            // ✅ MERGE LOGIC THÔNG MINH
                            for (Anime newAnime : newAnimes) {
                                Anime oldAnime = existingMap.get(newAnime.getId());
                                if (oldAnime != null) {
                                    // 1. Giữ lại các trường Detail nếu sync mới thiếu
                                    if (newAnime.getCharacters() == null)
                                        newAnime.setCharacters(oldAnime.getCharacters());
                                    if (newAnime.getRelations() == null)
                                        newAnime.setRelations(oldAnime.getRelations());
                                    if (newAnime.getStudios() == null)
                                        newAnime.setStudios(oldAnime.getStudios());
                                    if (newAnime.getRecommendations() == null)
                                        newAnime.setRecommendations(oldAnime.getRecommendations());
                                    if (newAnime.getTrailer() == null)
                                        newAnime.setTrailer(oldAnime.getTrailer());

                                    // 2. ✅ MERGE TITLE - Giữ field đã có, chỉ thêm field mới
                                    newAnime.setTitle(mergeTitles(oldAnime.getTitle(), newAnime.getTitle()));
                                }
                            }

                            animeRepository.saveAll(newAnimes);
                            log.info("Synced (Merged) {} items from {}", newAnimes.size(), filename);
                        }
                    }, error -> log.error("Error syncing file: " + filename, error));

        } catch (Exception e) {
            log.error("Failed to prepare sync for " + filename, e);
        }
    }

    // ✅ HÀM MERGE TITLE THÔNG MINH
    private Anime.Title mergeTitles(Anime.Title oldTitle, Anime.Title newTitle) {
        if (oldTitle == null) return newTitle;
        if (newTitle == null) return oldTitle;

        // Giữ field cũ nếu field mới là null
        if (newTitle.getRomaji() == null) newTitle.setRomaji(oldTitle.getRomaji());
        if (newTitle.getEnglish() == null) newTitle.setEnglish(oldTitle.getEnglish());
        if (newTitle.getUserPreferred() == null) newTitle.setUserPreferred(oldTitle.getUserPreferred());
        if (newTitle.getNativeTitle() == null) newTitle.setNativeTitle(oldTitle.getNativeTitle());

        return newTitle;
    }

    public Mono<Anime> fetchAndSaveAnimeDetail(String id) {
        log.info("Lazy Loading details for Anime ID: {}", id);
        return Mono.fromCallable(() -> loadGraphqlQuery("anime-info.graphql"))
                .flatMap(query -> webClient.post()
                        .bodyValue(Map.of("query", query, "variables", Map.of("id", Integer.parseInt(id))))
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .map(json -> {
                    JsonNode mediaNode = json.path("data").path("Media");
                    return mapJsonToEntity(mediaNode);
                })
                .flatMap(anime -> {
                    if (anime != null) {
                        // ✅ MERGE với data cũ trước khi save
                        return Mono.justOrEmpty(animeRepository.findById(anime.getId()))
                                .map(oldAnime -> {
                                    anime.setTitle(mergeTitles(oldAnime.getTitle(), anime.getTitle()));
                                    return anime;
                                })
                                .defaultIfEmpty(anime)
                                .map(animeRepository::save);
                    }
                    return Mono.empty();
                });
    }

    private Anime mapJsonToEntity(JsonNode node) {
        try {
            Anime anime = objectMapper.treeToValue(node, Anime.class);
            if (node.has("id")) {
                anime.setId(node.get("id").asText());
            }
            anime.setLastUpdated(LocalDateTime.now());
            return anime;
        } catch (Exception e) {
            log.error("Mapping error for node ID: " + node.path("id"), e);
            return null;
        }
    }

    public String loadGraphqlQuery(String filename) throws Exception {
        ClassPathResource resource = new ClassPathResource(filename);
        byte[] queryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(queryData, StandardCharsets.UTF_8);
    }

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

    // ✅ Sync Schedules mới: Lấy tất cả schedules trong 7 ngày tới, map và save vào DB

    @Scheduled(cron = "0 0 * * * ?")  // Mỗi 1 tiếng
    public void syncSchedules() {
        log.info("=== START SYNC SCHEDULES (7 days ahead) ===");

        long start = Instant.now().getEpochSecond();
        long end = Instant.now().plus(Duration.ofDays(7)).getEpochSecond();

        try {
            String query = loadGraphqlQuery("schedule-anime.graphql");

            // ✅ Dùng Flux.range() thay vì Flux.generate()
            Flux.range(1, 10)  // Max 10 pages
                    .concatMap(page -> fetchSchedulePage(query, page, 50, start, end))
                    .takeUntil(response -> {
                        // Stop khi không còn next page
                        boolean hasNextPage = response.path("data")
                                .path("Page")
                                .path("pageInfo")
                                .path("hasNextPage")
                                .asBoolean(false);
                        return !hasNextPage;
                    })
                    .flatMap(response -> {
                        // Parse schedules từ response
                        JsonNode schedulesNode = response.path("data")
                                .path("Page")
                                .path("airingSchedules");

                        if (!schedulesNode.isArray()) {
                            return Flux.empty();
                        }

                        List<AnimeSchedule> schedules = new ArrayList<>();
                        for (JsonNode node : schedulesNode) {
                            AnimeSchedule schedule = mapJsonToSchedule(node);
                            if (schedule != null) {
                                schedules.add(schedule);
                            }
                        }
                        return Flux.fromIterable(schedules);
                    })
                    .collectList()
                    .doOnNext(allSchedules -> {
                        if (!allSchedules.isEmpty()) {
                            // Xóa schedules cũ
                            scheduleRepository.deleteByAiringAtLessThan(start);

                            // Save tất cả schedules mới
                            List<AnimeSchedule> saved = scheduleRepository.saveAll(allSchedules);
                            log.info("✅ Synced {} schedules successfully", saved.size());
                        } else {
                            log.warn("⚠️ No schedules found");
                        }
                    })
                    .doOnError(error -> log.error("❌ Error syncing schedules", error))
                    .subscribe();  // Subscribe để trigger execution

        } catch (Exception e) {
            log.error("❌ Failed to sync schedules", e);
        }

        log.info("=== END SYNC SCHEDULES ===");
    }

    // ✅ Helper method: Fetch 1 page
    private Mono<JsonNode> fetchSchedulePage(String query, int page, int perPage, long start, long end) {
        Map<String, Object> variables = Map.of(
                "page", page,
                "perPage", perPage,
                "from", start,
                "to", end
        );

        return webClient.post()
                .bodyValue(Map.of("query", query, "variables", variables))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> log.error("Error fetching schedule page {}", page, error));
    }

    // ✅ Helper method: Map JSON to AnimeSchedule entity
    private AnimeSchedule mapJsonToSchedule(JsonNode node) {
        try {
            JsonNode mediaNode = node.path("media");
            String animeId = mediaNode.path("id").asText();
            Integer episode = node.path("episode").asInt();

            // Check tồn tại để tránh duplicate
            if (scheduleRepository.existsByAnimeIdAndEpisode(animeId, episode)) {
                return null;  // Skip nếu đã có
            }

            // Parse title
            Anime.Title title = new Anime.Title();
            JsonNode titleNode = mediaNode.path("title");
            title.setRomaji(titleNode.path("romaji").asText(null));
            title.setEnglish(titleNode.path("english").asText(null));
            title.setUserPreferred(titleNode.path("userPreferred").asText(null));
            title.setNativeTitle(titleNode.path("native").asText(null));

            // Parse other fields
            String coverImage = mediaNode.path("coverImage").path("large").asText(null);
            String bannerImage = mediaNode.path("bannerImage").asText(null);
            String format = mediaNode.path("format").asText(null);
            String status = mediaNode.path("status").asText(null);
            Integer episodes = mediaNode.path("episodes").isNull() ? null : mediaNode.path("episodes").asInt();

            // Calculate airing time
            long airingAt = node.path("airingAt").asLong();
            Instant airingInstant = Instant.ofEpochSecond(airingAt);
            ZonedDateTime zdt = airingInstant.atZone(ZoneId.systemDefault());

            String day = zdt.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("vi", "VN"));

            String airingTime = zdt.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            );

            // Set expiry 1 giờ sau khi phát sóng
            Date expiresAt = Date.from(airingInstant.plus(Duration.ofHours(1)));

            return AnimeSchedule.builder()
                    .animeId(animeId)
                    .episode(episode)
                    .airingAt(airingAt)
                    .airingDate(Date.from(airingInstant))
                    .day(day)
                    .airingTime(airingTime)
                    .title(title)
                    .coverImage(coverImage)
                    .bannerImage(bannerImage)
                    .format(format)
                    .status(status)
                    .episodes(episodes)
                    .fetchedAt(LocalDateTime.now())
                    .expiresAt(expiresAt)
                    .build();

        } catch (Exception e) {
            log.error("❌ Mapping error for schedule node", e);
            return null;
        }
    }
}