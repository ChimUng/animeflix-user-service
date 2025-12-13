package com.animeflix.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WatchHistoryResponse {
    private String id;

    // ========== ANIME INFO ==========
    private String aniId;
    private String aniTitle;
    private String image;

    // ========== EPISODE INFO ==========
    private String epId;
    private Integer epNum;
    private String epTitle;             // ✅ Tên tập

    // ========== WATCH PROGRESS ==========
    private Double timeWatched;         // Giây đã xem
    private Double duration;            // Tổng thời lượng
    private Double progress;            // 0.0 - 1.0
    private Boolean completed;

    // ========== NEXT EPISODE INFO ==========
    private String nextepId;            // ✅ ID tập tiếp theo
    private Integer nextepNum;          // ✅ Số tập tiếp theo

    // ========== PROVIDER & SETTINGS ==========
    private String provider;            // ✅ Provider
    private String subtype;             // ✅ sub/dub

    // ========== DEVICE INFO ==========
    private String device;
    private String quality;

    // ========== TIMESTAMPS ==========
    private LocalDateTime createdAt;    // Giống schema cũ
    private LocalDateTime updatedAt;
}