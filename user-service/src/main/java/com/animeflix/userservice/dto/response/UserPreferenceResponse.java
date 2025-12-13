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
public class UserPreferenceResponse {
    private String id;
    private Boolean enableNotifications;
    private Boolean notifyOnlyFavorites;
    private Boolean notifyRecommendations;
    private Boolean autoPlayNext;
    private String preferredQuality;
    private String preferredLanguage;
    private List<String> favoriteGenres;
    private List<String> excludedGenres;
    private Boolean publicProfile;
    private Boolean showWatchHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}