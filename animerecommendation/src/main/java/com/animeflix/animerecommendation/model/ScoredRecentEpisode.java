package com.animeflix.animerecommendation.model;

public class ScoredRecentEpisode extends RecentEpisode {
    private Integer score;

    // Getter and setter
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}