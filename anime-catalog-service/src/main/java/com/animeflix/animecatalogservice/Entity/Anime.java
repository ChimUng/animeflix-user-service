package com.animeflix.animecatalogservice.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "animes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anime {
    @Id
    @JsonProperty("id")
    private String id;
    private String idMal;
    private Title title;
    private CoverImage coverImage;
    private String description;
    private String bannerImage;
    private Integer episodes;
    private String status;
    private Integer duration;
    private List<String> genres;
    private String source;
    private String type;
    private Integer seasonYear;
    private String season;
    private String format;
    private Integer averageScore;
    private Integer popularity;
    private String countryOfOrigin;

    private NextAiringEpisode nextAiringEpisode;
    private FuzzyDate startDate;
    private FuzzyDate endDate;
    private Trailer trailer;

    // Các trường Connection quan trọng
    private StudioConnection studios;
    private RelationConnection relations;
    private RecommendationConnection recommendations;
    private CharacterConnection characters;

    private LocalDateTime lastUpdated;

    // --- 1. TITLE & IMAGE & DATE ---
    @Data
    public static class Title {
        private String romaji;
        private String english;
        private String userPreferred;
        @JsonProperty("native") // Map key "native" từ JSON
        private String nativeTitle;
    }

    @Data
    public static class CoverImage {
        private String large;
        private String extraLarge;
        private String color;
        private String medium;
    }

    @Data
    public static class FuzzyDate {
        private Integer year;
        private Integer month;
        private Integer day;
    }

    @Data
    public static class NextAiringEpisode {
        private Long airingAt;
        private Integer episode;
        private Integer timeUntilAiring;
    }

    @Data
    public static class Trailer {
        private String id;
        private String site;
        private String thumbnail;
    }

    // --- 2. STUDIOS ---
    @Data
    public static class StudioConnection {
        private List<StudioNode> nodes;
    }

    @Data
    public static class StudioNode {
        private String id;
        private String name;
        private String siteUrl;
    }

    // --- 3. RELATIONS ---
    @Data
    public static class RelationConnection {
        private List<RelationEdge> edges;
    }

    @Data
    public static class RelationEdge {
        @JsonProperty("relationType")
        private String relationType;
        private RelationNode node;
    }

    @Data
    public static class RelationNode {
        private String id;
        private Title title; // Dùng lại class Title ở trên
        private String format;
        private CoverImage coverImage; // Dùng lại class CoverImage ở trên
        private Integer episodes;
        private Integer chapters;
        private String status;
    }

    // --- 4. RECOMMENDATIONS ---
    @Data
    public static class RecommendationConnection {
        private List<RecommendationNode> nodes;
    }

    @Data
    public static class RecommendationNode {
        private RecommendationMedia mediaRecommendation;
    }

    @Data
    public static class RecommendationMedia {
        private String id;
        private Title title;
        private CoverImage coverImage;
        private Integer episodes;
        private String status;
        private String format;
        private NextAiringEpisode nextAiringEpisode;
    }

    // --- 5. CHARACTERS (Sửa kỹ phần Name) ---
    @Data
    public static class CharacterConnection {
        private List<CharacterEdge> edges;
    }

    @Data
    public static class CharacterEdge {
        private Integer id; // Anilist trả về Int cho Edge ID
        private String role;
        private CharacterNode node;
        private List<VoiceActorRole> voiceActorRoles;
    }

    @Data
    public static class CharacterNode {
        private String id;
        private Name name; // Object Name đầy đủ
        private Image image;
    }

    @Data
    public static class Name {
        private String first;
        private String last;
        private String full;
        @JsonProperty("native")
        private String nativeName;
        private String userPreferred;
    }

    @Data
    public static class Image {
        private String large;
        private String medium;
    }

    @Data
    public static class VoiceActorRole {
        private VoiceActor voiceActor;
    }

    @Data
    public static class VoiceActor {
        private String id;
        private Name name; // Dùng chung class Name với Character
        private Image image;
    }
}