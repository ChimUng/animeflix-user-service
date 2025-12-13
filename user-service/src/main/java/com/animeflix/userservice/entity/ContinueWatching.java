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

@Document(collection = "continue_watching")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_watched_idx", def = "{'userId': 1, 'lastWatchedAt': -1}")
@CompoundIndex(name = "user_anime_unique", def = "{'userId': 1, 'aniId': 1}", unique = true)
public class ContinueWatching {
    @Id
    private String id;

    @Indexed
    private String userId;

    // ========== ANIME INFO (Denormalized) ==========
    private String aniId;               // ✅ Dùng aniId (giống schema cũ)
    private String aniTitle;
    private String image;               // Cover image
    private Integer totalEpisodes;
    private String status;
    private String format;

    // ========== CURRENT EPISODE INFO ==========
    private String epId;                // Episode ID hiện tại
    private Integer epNum;              // Số tập hiện tại
    private String epTitle;             // ✅ Tên tập

    // ========== NEXT EPISODE INFO ==========
    private String nextepId;            // ✅ ID tập tiếp theo
    private Integer nextepNum;          // ✅ Số tập tiếp theo

    // ========== WATCH PROGRESS ==========
    private Double timeWatched;         // Giây đã xem
    private Double duration;            // Tổng thời lượng
    private Double progress;            // 0.0 - 1.0

    // ========== PROVIDER & SETTINGS ==========
    private String provider;            // ✅ Provider
    private String subtype;             // ✅ sub/dub

    // ========== TIMESTAMPS ==========
    @Indexed
    private LocalDateTime lastWatchedAt;
    private LocalDateTime createdAt;
}