package com.animeflix.userservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    // Notification settings
    @Builder.Default
    private Boolean enableNotifications = true;

    @Builder.Default
    private Boolean notifyOnlyFavorites = false;  // Chỉ thông báo anime yêu thích

    @Builder.Default
    private Boolean notifyRecommendations = true;

    // Watch preferences
    @Builder.Default
    private Boolean autoPlayNext = true;

    @Builder.Default
    private String preferredQuality = "1080p";    // "1080p", "720p", "480p"

    @Builder.Default
    private String preferredLanguage = "sub";     // "sub", "dub"

    // Content preferences
    @Builder.Default
    private List<String> favoriteGenres = new ArrayList<>();  // ["Action", "Comedy"]

    @Builder.Default
    private List<String> excludedGenres = new ArrayList<>();  // ["Horror"]

    // Privacy
    @Builder.Default
    private Boolean publicProfile = false;

    @Builder.Default
    private Boolean showWatchHistory = true;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
