package com.animeflix.animecatalogservice.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "anime_episode_idx", def = "{'animeId': 1, 'episode': 1}", unique = true)
public class AnimeSchedule {
    @Id
    private String id;

    private String animeId;
    private Integer episode;

    @Indexed
    private Long airingAt;

    @Indexed
    private Date airingDate;

    private String day;
    private String airingTime;

    // Denormalized anime data
    private Anime.Title title;
    private String coverImage;
    private String bannerImage;
    private String format;
    private String status;
    private Integer episodes;

    // Metadata
    private LocalDateTime fetchedAt;

    // TTL index - MongoDB sẽ tự động xóa document sau expiresAt
    @Indexed
    private Date expiresAt;
}