package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Model for raw episode data from external APIs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawEpisode {
    private String id;
    private String title;
    private String image;
    private String rating;
    private String description;
    private String url;
    private List<Source> sources;
    private List<Subtitle> subtitles;
    private Map<String, String> headers;
    private String download;
}