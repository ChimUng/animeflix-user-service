package com.animeflix.animeepisode.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider {
    private String providerId;
    private String id;

    @JsonIgnore
    private Boolean consumet;

    /**
     * ✅ FIX: episodes có thể là:
     * - List<Episode> (cho providers như AnimePahe, 9anime)
     * - Map<String, List<Episode>> (cho providers như Zoro, Gogoanime có sub/dub)
     *
     * Jackson cần biết deserialize thành type nào. Dùng custom deserializer.
     */
    @JsonDeserialize(using = EpisodesDeserializer.class)
    private Object episodes;

    public Provider(String providerId, String id, Object episodes) {
        this.providerId = providerId;
        this.id = id;
        this.consumet = false;  // Default = false
        this.episodes = episodes;
    }
}