package com.animeflix.animetranslate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranslationRequest {
    @JsonProperty("anilistId")
    private Integer anilistId;
    private String title;
    private String description;

    // Constructors, getters, setters
    public TranslationRequest() {}
    public TranslationRequest(Integer anilistId, String title, String description) {
        this.anilistId = anilistId;
        this.title = title;
        this.description = description;
    }

    public Integer getAnilistId() { return anilistId; }
    public void setAnilistId(Integer anilistId) { this.anilistId = anilistId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}