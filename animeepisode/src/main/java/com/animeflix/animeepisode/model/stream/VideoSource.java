package com.animeflix.animeepisode.model.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSource {
    private String url;
    private String quality;
    private Boolean isM3U8;
    private String type; // "hls", etc.
}