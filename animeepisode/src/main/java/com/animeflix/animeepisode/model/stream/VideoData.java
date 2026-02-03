package com.animeflix.animeepisode.model.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Main stream response body.
 * Matches Next.js VideoData interface 1:1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoData {
    private List<VideoSource> sources;
    private List<VideoTrack> tracks;
    private VideoTimeRange intro;
    private VideoTimeRange outro;
    private Map<String, String> headers;
}
