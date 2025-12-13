package com.animeflix.userservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddHistoryRequest {

    // ========== ANIME INFO ==========
    @NotBlank(message = "Anime ID is required")
    private String aniId;               // Anime ID

    private String aniTitle;            // Tên anime (optional, sẽ fetch nếu null)
    private String image;               // Cover image (optional)

    // ========== EPISODE INFO ==========
    @NotBlank(message = "Episode ID is required")
    private String epId;                // Episode ID đầy đủ

    @NotNull(message = "Episode number is required")
    @Min(1)
    private Integer epNum;              // Số tập

    private String epTitle;             // ✅ Tên tập (optional)

    // ========== WATCH PROGRESS ==========
    @Min(0)
    private Double timeWatched;         // Giây đã xem

    @Min(0)
    private Double duration;            // Tổng thời lượng (giây)

    private Boolean completed;          // Xem xong chưa

    // ========== NEXT EPISODE INFO ==========
    private String nextepId;            // ✅ ID tập tiếp theo
    private Integer nextepNum;          // ✅ Số tập tiếp theo

    // ========== PROVIDER & SETTINGS ==========
    private String provider;            // ✅ "gogoanime", "zoro", "animepahe"

    @Builder.Default
    private String subtype = "sub";     // ✅ "sub" hoặc "dub"

    // ========== DEVICE INFO ==========
    private String device;              // "web", "mobile", "tv"
    private String quality;             // "1080p", "720p", "480p"
}