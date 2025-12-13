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
public class FavoriteResponse {
    private String id;
    private String animeId;
    private Boolean notifyNewEpisode;
    private LocalDateTime addedAt;

    // Denormalized anime data
    private String animeTitle;
    private String coverImage;
    private String bannerImage;
    private String status;
    private Integer totalEpisodes;
}