package com.animeflix.animerecent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;


@Data
public class RecentResponse {
    private DataWrapper data;

    @Data
    public static class DataWrapper {
        private Page Page;
    }

    @Data
    public static class Page {
        private List<Media> media;
    }

    @Data
    public static class Media {
        private int id;
        private int idMal;
        private Title title;
        private CoverImage coverImage;
        private String format;
        private String status;
        private Integer episodes;
        private NextAiringEpisode nextAiringEpisode;
    }

    @Data
    public static class Title {
        private String romaji;
        private String english;
    }

    @Data
    public static class CoverImage {
        private String extraLarge;
        private String large;
        private String medium;
        private String color;
    }

    @Data
    public static class NextAiringEpisode {
        private Integer episode;
        private Long airingAt;
    }
}