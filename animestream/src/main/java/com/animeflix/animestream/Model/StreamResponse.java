package com.animeflix.animestream.Model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Model for stream response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponse {
    private boolean success;
    private Data data;

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private Map<String, String> headers;
        private List<Source> sources;
        private List<Subtitle> subtitles;
        private Integer anilistID;
        private Integer malID;
    }
}