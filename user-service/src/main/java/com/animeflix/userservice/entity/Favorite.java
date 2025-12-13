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

@Document(collection = "favorites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_anime_unique", def = "{'userId': 1, 'animeId': 1}", unique = true)
@CompoundIndex(name = "notify_added_idx", def = "{'notifyNewEpisode': 1, 'addedAt': -1}")
public class Favorite {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String animeId;

    // Notification settings
    private Boolean notifyNewEpisode;

    // Denormalized anime data (để tránh gọi catalog-service mỗi lần)
    private String animeTitle;
    private String coverImage;
    private String bannerImage;
    private String status;          // "RELEASING", "FINISHED"
    private Integer totalEpisodes;

    @Indexed
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
}
