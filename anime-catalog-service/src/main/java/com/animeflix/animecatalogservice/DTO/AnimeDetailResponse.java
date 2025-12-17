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
public class AnimeDetailResponse {
    private String id;
    private String idMal;
    private AnimeResponse.Title title;
    private AnimeResponse.CoverImage coverImage;
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
    private String countryOfOrigin;

    private DateDTO startDate;
    private DateDTO endDate;
    private AnimeResponse.NextAiring nextAiringEpisode;
    private TrailerDTO trailer;

    private List<StudioDTO> studios;
    private List<RelationDTO> relations;
    private List<CharacterDTO> characters;
    private List<AnimeResponse> recommendations;

    // --- INNER CLASSES ---

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DateDTO {
        private Integer year;
        private Integer month;
        private Integer day;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrailerDTO {
        private String id;
        private String site;
        private String thumbnail;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudioDTO {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RelationDTO {
        private String relationType;
        private String id;
        private AnimeResponse.Title title;
        private AnimeResponse.CoverImage coverImage;
        private String format;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CharacterDTO {
        private String id;
        private String role;
        private String name;
        private String image;
        private String voiceActorName;
        private String voiceActorImage;
    }
}