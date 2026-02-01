package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider {
    private String providerId;
    private String id;
    @JsonIgnore
    private Boolean consumet;
    private Object episodes;  // "sub" -> List<Episode>, "dub" -> List<Episode>

    public Provider(String providerId, String id, Object episodes) {
        this.providerId = providerId;
        this.id = id;
        this.consumet = false;  // Default = false
        this.episodes = episodes;
    }
}