package com.animeflix.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatsResponse {
    private Long totalAnimeWatched;
    private Long totalEpisodesWatched;
    private Long totalWatchTimeSeconds;
    private Long favoritesCount;
    private Long unreadNotifications;
    private List<String> topGenres;         // Top 3 favorite genres
    private LocalDateTime memberSince;
}