package com.animeflix.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "watch_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_anime_idx", def = "{'userId': 1, 'aniId': 1}")
@CompoundIndex(name = "user_watched_idx", def = "{'userId': 1, 'createdAt': -1}")
public class WatchHistory {
    @Id
    private String id;

    @Indexed
    private String userId;
    private String aniId;
    private String aniTitle;
    private String image;

    private String epId;
    private Integer epNum;
    private String epTitle;

    private Double timeWatched;
    private Double duration;
    private Double progress;
    private Boolean completed;

    private String nextepId;
    private Integer nextepNum;

    private String provider;
    private String subtype;

    private String device;              // "web", "mobile", "tv"
    private String quality;             // "1080p", "720p", "480p"

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}