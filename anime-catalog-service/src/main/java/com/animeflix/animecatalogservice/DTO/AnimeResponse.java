package com.animeflix.animecatalogservice.DTO;

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
public class AnimeResponse {
    private String id;
    private String idMal;
    private Title title;
    private CoverImage coverImage;
    private String bannerImage;
    private String description;
    private List<String> genres;
    private Integer episodes;
    private String status;
    private Integer duration;
    private Integer averageScore;
    private Integer popularity;
    private String season;
    private Integer seasonYear;
    private String format;
    private NextAiring nextAiringEpisode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // Ẩn field null trong Title
    public static class Title {
        private String romaji;
        private String english;
        private String userPreferred;
        private String nativeTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CoverImage {
        private String extraLarge;
        private String large;
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NextAiring {
        private Integer episode;
        private Long airingAt;
    }
}