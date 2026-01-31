package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AnimePahe Episode Data
 * Episode từ Consumet API response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimePaheEpisodeData {

    @JsonProperty("id")
    private String id;          // UUID/hash đầy đủ (e.g., "d58fc9f8.../f3316203...")

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("title")
    private String title;       // Optional
}