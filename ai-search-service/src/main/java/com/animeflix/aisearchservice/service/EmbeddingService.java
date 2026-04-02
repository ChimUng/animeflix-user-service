package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.client.GeminiClient;
import com.animeflix.aisearchservice.dto.kafka.AnimeUpdatedEvent;
import com.animeflix.aisearchservice.Entity.AnimeVector;
import com.animeflix.aisearchservice.Repository.AnimeVectorRepository;
import com.animeflix.aisearchservice.util.TextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service xử lý việc embed và lưu vector cho một anime.
 * Được gọi từ:
 * 1. EmbeddingConsumerService (Kafka consumer - real-time update)
 * 2. EmbeddingBatchService (batch job - lần đầu setup)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final GeminiClient geminiClient;
    private final AnimeVectorRepository animeVectorRepository;

    /**
     * Embed anime từ Kafka event và upsert vào MongoDB.
     */
    public Mono<AnimeVector> embedAndSave(AnimeUpdatedEvent event) {
        String cleanDescription = TextUtils.stripHtml(event.getDescription());
        String embedText = buildEmbedText(
                event.getTitleRomaji(),
                cleanDescription,
                event.getGenres(),
                event.getTags()
        );

        return geminiClient.embed(embedText)
                .flatMap(vector -> {
                    // Upsert: nếu đã có → update vector + metadata
                    return animeVectorRepository.findById(event.getAnimeId())
                            .defaultIfEmpty(AnimeVector.builder()
                                    .id(event.getAnimeId())
                                    .build())
                            .flatMap(existing -> {
                                AnimeVector updated = AnimeVector.builder()
                                        .id(event.getAnimeId())
                                        .titleRomaji(event.getTitleRomaji())
                                        .titleEnglish(event.getTitleEnglish())
                                        .description(event.getDescription())
                                        .cleanDescription(cleanDescription)
                                        .genres(event.getGenres())
                                        .tags(event.getTags())
                                        .coverImageLarge(event.getCoverImageLarge())
                                        .coverImageExtraLarge(event.getCoverImageExtraLarge())
                                        .bannerImage(event.getBannerImage())
                                        .averageScore(event.getAverageScore())
                                        .popularity(event.getPopularity())
                                        .status(event.getStatus())
                                        .format(event.getFormat())
                                        .season(event.getSeason())
                                        .seasonYear(event.getSeasonYear())
                                        .descriptionVector(vector)
                                        .embeddedText(embedText)
                                        .embeddedAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .embedded(true)
                                        .build();

                                return animeVectorRepository.save(updated);
                            });
                })
                .doOnSuccess(saved -> log.info("✅ Embedded anime: {}", event.getAnimeId()))
                .doOnError(e -> log.error("❌ Embed failed for anime {}: {}", event.getAnimeId(), e.getMessage()));
    }

    /**
     * Build text để embed - kết hợp title + description + genres + tags.
     * Chất lượng embedding phụ thuộc vào chất lượng text này.
     */
    public String buildEmbedText(String title, String description, List<String> genres, List<String> tags) {
        StringBuilder sb = new StringBuilder();

        if (title != null) sb.append(title).append(". ");
        if (description != null && !description.isBlank()) {
            // Giới hạn description để tránh vượt token limit Gemini
            String truncated = description.length() > 1000
                    ? description.substring(0, 1000) + "..."
                    : description;
            sb.append(truncated).append(". ");
        }
        if (genres != null && !genres.isEmpty()) {
            sb.append("Thể loại: ").append(String.join(", ", genres)).append(". ");
        }
        if (tags != null && !tags.isEmpty()) {
            sb.append("Tags: ").append(String.join(", ", tags));
        }

        return sb.toString().trim();
    }
}