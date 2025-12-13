package com.animeflix.userservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {
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
}
