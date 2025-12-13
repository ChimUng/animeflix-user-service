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
public class ContinueWatchingResponse {
    private String id;

    // ========== ANIME INFO ==========
    private String aniId;
    private String aniTitle;
    private String image;
    private Integer totalEpisodes;
    private String status;
    private String format;

    // ========== CURRENT EPISODE INFO ==========
    private String epId;
    private Integer epNum;
    private String epTitle;             // ✅ Tên tập

    // ========== NEXT EPISODE INFO ==========
    private String nextepId;            // ✅ ID tập tiếp theo
    private Integer nextepNum;          // ✅ Số tập tiếp theo

    // ========== WATCH PROGRESS ==========
    private Double timeWatched;
    private Double duration;
    private Double progress;            // 0.0 - 1.0

    // ========== PROVIDER & SETTINGS ==========
    private String provider;            // ✅ "gogoanime", "zoro"
    private String subtype;             // ✅ "sub" / "dub"

    // ========== TIMESTAMP ==========
    private LocalDateTime lastWatchedAt;
}