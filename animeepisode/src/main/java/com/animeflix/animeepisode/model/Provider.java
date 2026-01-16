package com.animeflix.animeepisode.model;

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
    private Boolean consumet;
    private Object episodes;  // "sub" -> List<Episode>, "dub" -> List<Episode>
}