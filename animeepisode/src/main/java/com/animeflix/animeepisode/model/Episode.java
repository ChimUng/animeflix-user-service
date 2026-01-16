package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Episode {
    @JsonProperty("id") private String id;
    @JsonProperty("episodeId") private String episodeId;
    @JsonProperty("number") private Integer number;
    @JsonProperty("title") private String title;
    @JsonProperty("description") private String description;
    @JsonProperty("img") private String img;
    @JsonProperty("image") private String image;
    @JsonProperty("url") private String url;  // From RawEpisode
    @JsonProperty("isFiller") private Boolean isFiller;
}