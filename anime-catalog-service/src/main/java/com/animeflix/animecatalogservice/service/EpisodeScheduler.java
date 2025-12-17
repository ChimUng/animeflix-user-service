package com.animeflix.animecatalogservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpisodeScheduler {

    private final AnimeService animeService;
    private final EpisodeEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PUBLISHED_KEY_PREFIX = "published:";

    /**
     * Check new episodes every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkNewEpisodes() {
        log.info("🔍 Checking for new episodes...");

        long startTime = System.currentTimeMillis();
        long now = Instant.now().getEpochSecond();
        long next24h = Instant.now().plus(Duration.ofHours(24)).getEpochSecond();

        animeService.getAnimeSchedule(1, 50, now)
                .subscribe(
                        scheduleJson -> processSchedule(scheduleJson),
                        error -> log.error("❌ Error checking episodes: {}", error.getMessage()),
                        () -> {
                            long duration = System.currentTimeMillis() - startTime;
                            log.info("✅ Episode check completed in {}ms", duration);
                        }
                );
    }

    /**
     * Process schedule response and publish events
     */
    private void processSchedule(String scheduleJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(scheduleJson);
            JsonNode animes = root.path("animes");

            if (!animes.isArray()) {
                log.warn("⚠️ Invalid schedule response format");
                return;
            }

            int publishedCount = 0;

            for (JsonNode anime : animes) {
                String animeId = anime.path("id").asText();
                Integer episodeNum = anime.path("episode").asInt();

                // Check if already published
                String cacheKey = PUBLISHED_KEY_PREFIX + animeId + ":" + episodeNum;
                Boolean exists = redisTemplate.hasKey(cacheKey);

                if (Boolean.TRUE.equals(exists)) {
                    log.debug("⏭️ Already published: anime={}, episode={}", animeId, episodeNum);
                    continue;
                }

                // Parse anime details
                JsonNode title = anime.path("title");
                String animeTitle = title.path("english").asText();
                if (animeTitle == null || animeTitle.isEmpty()) {
                    animeTitle = title.path("romaji").asText();
                }

                Long airingAt = anime.path("airingAt").asLong();
                String coverImage = anime.path("coverImage").asText();
                String bannerImage = anime.path("bannerImage").asText();

                // Publish event
                eventPublisher.publishNewEpisode(
                        animeId,
                        animeTitle,
                        episodeNum,
                        airingAt,
                        coverImage,
                        bannerImage
                );

                // Mark as published (TTL: 30 days)
                redisTemplate.opsForValue().set(cacheKey, "1", 30, TimeUnit.DAYS);
                publishedCount++;
            }

            log.info("📤 Published {} new episode events", publishedCount);

        } catch (Exception e) {
            log.error("❌ Error processing schedule: {}", e.getMessage(), e);
        }
    }
}