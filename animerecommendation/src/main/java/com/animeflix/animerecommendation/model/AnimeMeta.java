package com.animeflix.animerecommendation.model;

import java.util.ArrayList;
import java.util.List;

public class AnimeMeta {
    private Integer id;
    private Title title;
    private List<String> genres = new ArrayList<>();
    private List<Tag> tags;

    // Getters and setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Title getTitle() { return title; }
    public void setTitle(Title title) { this.title = title; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    public static class Title {
        private String romaji;
        private String english;

        // Getters and setters
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }

        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }
    }

    public static class Tag {
        private String name;

        // Getter and setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}