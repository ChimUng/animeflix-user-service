package com.animeflix.aisearchservice.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document lưu vector embedding của anime.
 * ID = anilistId để dễ join/lookup với catalog service.
 * Tách hoàn toàn khỏi collection "animes" của catalog service.
 *
 * Vector index phải tạo thủ công trên MongoDB Atlas:
 * {
 *   "fields": [{
 *     "type": "vector",
 *     "path": "descriptionVector",
 *     "numDimensions": 768,
 *     "similarity": "cosine"
 *   }]
 * }
 */

@Document(collection = "anime_vectors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimeVector {

    @Id
    private String id;

    private String titleRomaji;
    private String titleEnglish;
    private String titleUserPreferred;
    private String coverImageLarge;
    private String coverImageExtraLarge;
    private String bannerImage;
    private String description;
    private String cleanDescription;

    @Indexed
    private List<String> genres;

    private List<String> tags;
    private Integer averageScore;
    private Integer popularity;
    private String status;
    private String format;
    private String season;
    private Integer seasonYear;

    // --- Vector field (768 dims từ Gemini text-embedding-004) ---
    private List<Double> descriptionVector;

    // --- Metadata ---
    private String embeddedText;
    private LocalDateTime embeddedAt;  // Lần cuối embed
    private LocalDateTime updatedAt;   // Lần cuối update metadata từ catalog
    private Boolean embedded;
}
