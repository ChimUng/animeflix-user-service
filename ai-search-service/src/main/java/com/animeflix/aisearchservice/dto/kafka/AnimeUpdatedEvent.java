package com.animeflix.aisearchservice.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Event catalog-service publish lên Kafka khi sync anime mới hoặc update.
 * AI search service lắng nghe event này để tự động embed lại.
 *
 * Catalog service cần thêm vào AnimeSyncService:
 *   kafkaTemplate.send("anime.data.updated", animeId, AnimeUpdatedEvent.builder()...build());
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimeUpdatedEvent {
    private String eventId;
    private String animeId;
    private String titleRomaji;
    private String titleEnglish;
    private String description;       // Raw HTML description từ AniList
    private List<String> genres;
    private List<String> tags;
    private String coverImageLarge;
    private String coverImageExtraLarge;
    private String bannerImage;
    private Integer averageScore;
    private Integer popularity;
    private String status;
    private String format;
    private String season;
    private Integer seasonYear;
    private Long timestamp;
}