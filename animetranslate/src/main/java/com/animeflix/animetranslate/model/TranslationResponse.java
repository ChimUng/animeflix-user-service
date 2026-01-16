package com.animeflix.animetranslate.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranslationResponse {
    @JsonProperty("anilistId")
    private Integer anilistId;
    @JsonProperty("title_vi")
    private String titleVi;
    @JsonProperty("description_vi")
    private String descriptionVi;
    private String error;

    // Constructors, getters, setters
    public TranslationResponse() {}
    public TranslationResponse(Integer anilistId, String titleVi, String descriptionVi) {
        this.anilistId = anilistId;
        this.titleVi = titleVi;
        this.descriptionVi = descriptionVi;
    }

    public Integer getAnilistId() { return anilistId; }
    public void setAnilistId(Integer anilistId) { this.anilistId = anilistId; }
    public String getTitleVi() { return titleVi; }
    public void setTitleVi(String titleVi) { this.titleVi = titleVi; }
    public String getDescriptionVi() { return descriptionVi; }
    public void setDescriptionVi(String descriptionVi) { this.descriptionVi = descriptionVi; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}