package com.animeflix.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationResponse {
    private List<AnimeRecommendation> recommendations;
    private String reason;          // "Based on your watch history"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnimeRecommendation {
        private String id;
        private String title;
        private String coverImage;
        private String bannerImage;
        private List<String> genres;
        private Integer averageScore;
        private Integer popularity;
        private String status;
        private String format;
        private Integer score;      // Recommendation score (0-100)
        private String matchReason; // "Matches your favorite genre: Action"
    }
}
