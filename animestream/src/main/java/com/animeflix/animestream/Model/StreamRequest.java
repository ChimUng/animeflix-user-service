package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model for stream request parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    private String source;
    private String provider;
    private String episodeId;
    private int episodeNum;
    private String subtype;
}