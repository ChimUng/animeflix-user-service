package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model for video source data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {
    private String url;
    private boolean isM3U8;
    private String quality;

    public void setIsM3U8(boolean isM3U8) {

    }
}