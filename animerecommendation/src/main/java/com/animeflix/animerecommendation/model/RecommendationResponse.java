package com.animeflix.animerecommendation.model;

import java.util.List;

public class RecommendationResponse {
    private List<ScoredRecentEpisode> recommendations;

    public RecommendationResponse(List<ScoredRecentEpisode> recommendations) {
        this.recommendations = recommendations;
    }

    // Getter and setter
    public List<ScoredRecentEpisode> getRecommendations() { return recommendations; }
    public void setRecommendations(List<ScoredRecentEpisode> recommendations) { this.recommendations = recommendations; }
}