package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.model.stream.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anify stream client.
 * GET https://api.anify.tv/sources
 *   ?providerId=${provider}
 *   &watchId=${episodeid}
 *   &episodeNumber=${epnum}
 *   &id=${animeId}
 *   &subType=${subtype}
 *
 * Matches Next.js AnifyEpisode().
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnifyStreamClient {

    private final WebClient anifyWebClient;

    /**
     * @param provider   provider ID (e.g. "zoro", "gogoanime")
     * @param episodeid  watchId / episode ID
     * @param episodenum episode number
     * @param animeId    anilist ID
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetchAnifyStream(
            String provider,
            String episodeid,
            String episodenum,
            String animeId,
            String subtype
    ) {
        log.info("🔍 [Anify] provider={}, episodeid={}, epnum={}, id={}, subtype={}",
                provider, episodeid, episodenum, animeId, subtype);

        return anifyWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/sources")
                        .queryParam("providerId", provider)
                        .queryParam("watchId", episodeid)
                        .queryParam("episodeNumber", episodenum)
                        .queryParam("id", animeId)
                        .queryParam("subType", subtype)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    log.info("✅ [Anify] Got response");
                    return parseVideoData(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ [Anify] error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    // ========================================
    // Anify response structure:
    // {
    //   "sources": [{ "url", "quality", "isM3U8" }],
    //   "subtitles": [{ "url", "language" }],   ← NOTE: Anify dùng "language" (không phải "lang")
    //   "headers": { "Referer": "..." }
    // }
    // ========================================
    private VideoData parseVideoData(JsonNode response) {
        VideoData videoData = new VideoData();

        // sources
        List<VideoSource> sources = new ArrayList<>();
        response.path("sources").forEach(s -> {
            sources.add(new VideoSource(
                    s.path("url").asText(""),
                    s.path("quality").asText(null),
                    s.path("isM3U8").asBoolean(false),
                    null
            ));
        });
        videoData.setSources(sources);

        // subtitles -> tracks (Anify dùng "language", map sang "lang")
        List<VideoTrack> tracks = new ArrayList<>();
        response.path("subtitles").forEach(s -> {
            tracks.add(new VideoTrack(
                    s.path("url").asText(""),
                    s.path("language").asText(""),  // ✅ Anify dùng "language"
                    "subtitles",
                    false
            ));
        });
        if (!tracks.isEmpty()) videoData.setTracks(tracks);

        // headers
        Map<String, String> headers = new HashMap<>();
        response.path("headers").fields().forEachRemaining(entry ->
                headers.put(entry.getKey(), entry.getValue().asText(""))
        );
        if (!headers.isEmpty()) videoData.setHeaders(headers);

        return videoData;
    }
}
