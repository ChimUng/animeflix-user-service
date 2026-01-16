package com.animeflix.animeepisode.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AnifyProvider {
    private String providerId;
    private List<AnifyEpisode> episodes;
}

/*
    example:
    {
        "providerId":"zoro"
        "episodes":[
            {
            "id":"21"
            "episodeId":"1044"
            .....
            ->remain props key in AnifyEpisode DTO
            }
        ]
    }
 */