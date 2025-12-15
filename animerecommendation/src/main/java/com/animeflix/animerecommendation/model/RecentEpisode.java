package com.animeflix.animerecommendation.model;

import java.util.List;

public class RecentEpisode {
    private String id;
    private Title title;
    private String status;
    private String format;
    private Integer totalEpisodes;
    private Integer currentEpisode;
    private CoverImage coverImage;
    private String latestEpisode;
    private List<String> genres;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Title getTitle() { return title; }
    public void setTitle(Title title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getTotalEpisodes() { return totalEpisodes; }
    public void setTotalEpisodes(Integer totalEpisodes) { this.totalEpisodes = totalEpisodes; }

    public Integer getCurrentEpisode() { return currentEpisode; }
    public void setCurrentEpisode(Integer currentEpisode) { this.currentEpisode = currentEpisode; }

    public CoverImage getCoverImage() { return coverImage; }
    public void setCoverImage(CoverImage coverImage) { this.coverImage = coverImage; }

    public String getLatestEpisode() { return latestEpisode; }
    public void setLatestEpisode(String latestEpisode) { this.latestEpisode = latestEpisode; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public static class Title {
        private String romaji;
        private String english;
        private String native_;

        // Getters and setters
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }

        public String getEnglish() { return english; }
        public void setEnglish(String english) { this.english = english; }

        public String getNative_() { return native_; }
        public void setNative_(String native_) { this.native_ = native_; }
    }

    public static class CoverImage {
        private String large;
        private String medium;

        // Getters and setters
        public String getLarge() { return large; }
        public void setLarge(String large) { this.large = large; }

        public String getMedium() { return medium; }
        public void setMedium(String medium) { this.medium = medium; }
    }
}