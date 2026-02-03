package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EpisodeMeta {// keys bên trong entry episode của api ani.zip
    @JsonProperty("episode")
    private String episode;
    @JsonProperty("title")
    private Map<String, String> title;
//    @JsonProperty("overview")
//    private String overview;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("image")
    private String image;
}
