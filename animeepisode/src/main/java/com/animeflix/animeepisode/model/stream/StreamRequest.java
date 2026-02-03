package com.animeflix.animeepisode.model.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POST body từ frontend.
 * Matches Next.js RequestBody interface:
 *   { source, provider, episodeid, episodenum, subtype }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamRequest {
    private String source;       // "consumet" | "anify"
    private String provider;     // "zoro" | "9anime" | "animepahe" | "gogoanime" | ...
    private String episodeid;    // episode ID từ provider
    private String episodenum;   // episode number (string vì Next.js gửi number | string)
    private String subtype;      // "sub" | "dub"
}