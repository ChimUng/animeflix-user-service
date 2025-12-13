package com.animeflix.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteRequest {
    @NotBlank(message = "Anime ID is required")
    private String animeId;

    @Builder.Default
    private Boolean notifyNewEpisode = true;

    // Denormalized data (optional - sẽ fetch từ catalog-service nếu null)
    private String animeTitle;
    private String coverImage;
    private String bannerImage;
    private String status;
    private Integer totalEpisodes;
}