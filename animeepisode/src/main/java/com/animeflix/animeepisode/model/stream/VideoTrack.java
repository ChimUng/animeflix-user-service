package com.animeflix.animeepisode.model.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoTrack {
    private String url;
    private String lang;
    private String kind;
    private Boolean isDefault;
}