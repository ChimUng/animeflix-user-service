package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AnimePahe Search Result
 * Response từ Consumet API: /anime/animepahe/{title}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimePaheSearchResult {

    @JsonProperty("id")
    private String id;              // UUID của anime

    @JsonProperty("title")
    private String title;           // Tên anime

    @JsonProperty("type")
    private String type;            // TV, Movie, OVA

    @JsonProperty("releaseDate")
    private Integer releaseDate;    // Năm phát hành

    @JsonProperty("rating")
    private Integer rating;         // Optional
}