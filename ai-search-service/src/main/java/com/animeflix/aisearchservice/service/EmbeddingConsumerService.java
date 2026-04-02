package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.dto.kafka.AnimeUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

/**
 * Lắng nghe Kafka event từ anime-catalog-service.
 * Khi có anime mới/update → tự động embed lại vector.
 *
 * Catalog service cần publish event này trong AnimeSyncService.saveAll() xong.
 * Topic: anime.data.updated
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingConsumerService {

    private final EmbeddingService embeddingService;

    @KafkaListener(
            topics = "${kafka.topics.anime-updated}",
            groupId = "ai-search-service",
            containerFactory = "animeUpdatedListenerFactory"
    )
    public void onAnimeUpdated(AnimeUpdatedEvent event, Acknowledgment ack) {
        log.info("📨 Received anime.data.updated event: animeId={}", event.getAnimeId());

        embeddingService.embedAndSave(event)
                .doOnSuccess(saved -> {
                    log.info("✅ Re-embedded anime: {}", event.getAnimeId());
                    ack.acknowledge();  // Manual commit sau khi xử lý xong
                })
                .doOnError(e -> {
                    log.error("❌ Failed to embed anime {}: {}", event.getAnimeId(), e.getMessage());
                    // Không ack → Kafka sẽ retry
                })
                .subscribe();
    }
}