package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnifyEpisode {
    @JsonProperty("id") private String id; // id anime
    @JsonProperty("episodeId") private String episodeId; // id anime episode
    @JsonProperty("number") private Integer number; // episode number
    @JsonProperty("title") private String title; // title not same description !!!
    @JsonProperty("isFiller") private Boolean isFiller; // special episode
    @JsonProperty("image") private String image;
    @JsonProperty("description") private String description; //descrip per episode
//    [....] also need more fiends
}