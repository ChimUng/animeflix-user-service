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
    private String userId;              // userId từ JWT (thay vì userName)

    // ========== ANIME INFO (Denormalized) ==========
    private String aniId;               // Anime ID (giống schema cũ)
    private String aniTitle;            // Tên anime
    private String image;               // Cover image của anime

    // ========== EPISODE INFO ==========
    private String epId;                // Episode ID đầy đủ
    private Integer epNum;              // Số tập
    private String epTitle;             // ✅ TÊN TẬP (THIẾU TRƯỚC ĐÂY!)

    // ========== WATCH PROGRESS ==========
    private Double timeWatched;         // Giây đã xem (thay vì watchedSeconds)
    private Double duration;            // Tổng thời lượng tập (giây)
    private Double progress;            // 0.0 - 1.0 (calculated: timeWatched/duration)
    private Boolean completed;          // Xem xong chưa

    // ========== NEXT EPISODE INFO (QUAN TRỌNG CHO CONTINUE WATCHING!) ==========
    private String nextepId;            // ✅ ID tập tiếp theo
    private Integer nextepNum;          // ✅ Số tập tiếp theo

    // ========== PROVIDER & SETTINGS ==========
    private String provider;            // ✅ "gogoanime", "zoro", "animepahe"
    private String subtype;             // ✅ "sub" hoặc "dub"

    // ========== DEVICE INFO ==========
    private String device;              // "web", "mobile", "tv"
    private String quality;             // "1080p", "720p", "480p"

    // ========== TIMESTAMPS ==========
    @Indexed
    private LocalDateTime createdAt;    // Thời điểm xem (giống schema cũ)
    private LocalDateTime updatedAt;    // Last update
}