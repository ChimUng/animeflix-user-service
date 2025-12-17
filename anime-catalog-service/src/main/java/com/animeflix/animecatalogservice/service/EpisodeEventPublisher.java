package com.animeflix.animecatalogservice.service;

import com.animeflix.animecatalogservice.DTO.kafka.NewEpisodeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeEventPublisher {

    private final KafkaTemplate<String, NewEpisodeEvent> kafkaTemplate;

    private static final String TOPIC = "anime.episode.new";

    /**
     * Publish new episode event to Kafka
     *
     * @param animeId Anime ID (used as Kafka key for partitioning)
     * @param animeTitle Anime title
     * @param episodeNumber Episode number
     * @param airingAt Airing timestamp
     * @param coverImage Cover image URL
     * @param bannerImage Banner image URL
     */
    public void publishNewEpisode(
            String animeId,
            String animeTitle,
            Integer episodeNumber,
            Long airingAt,
            String coverImage,
            String bannerImage) {

        NewEpisodeEvent event = NewEpisodeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .animeId(animeId)
                .animeTitle(animeTitle)
                .episodeNumber(episodeNumber)
                .airingAt(airingAt)
                .coverImage(coverImage)
                .bannerImage(bannerImage)
                .timestamp(System.currentTimeMillis())
                .source("anilist")
                .build();

        publishEvent(animeId, event);
    }

    /**
     * Publish event with callback
     */
    private void publishEvent(String key, NewEpisodeEvent event) {
        log.info("📤 Publishing new episode event: anime={}, episode={}",
                event.getAnimeId(), event.getEpisodeNumber());

        CompletableFuture<SendResult<String, NewEpisodeEvent>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Event published successfully: " +
                                "topic={}, partition={}, offset={}, key={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            } else {
                log.error("❌ Failed to publish event: anime={}, episode={}, error={}",
                        event.getAnimeId(),
                        event.getEpisodeNumber(),
                        ex.getMessage());
            }
        });
    }
}