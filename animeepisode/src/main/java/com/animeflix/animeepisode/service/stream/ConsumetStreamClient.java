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
 * Consumet stream client.
 * GET ${CONSUMET_URI}/meta/anilist/watch/${episodeid}
 * -> trả về sources, subtitles, headers.
 *
 * Matches Next.js consumetEpisode() — response trả về trực tiếp từ Consumet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumetStreamClient {

    private final WebClient consumetWebClient;

    /**
     * @param episodeid  episode ID (e.g. "one-piece-episode-1" format từ gogoanime)
     */
    public Mono<VideoData> fetchConsumetStream(String episodeid) {
        log.info("🔍 [Consumet] fetching watch for episodeId: {}", episodeid);

        return consumetWebClient.get()
                .uri("/meta/anilist/watch/" + episodeid)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    log.info("✅ [Consumet] Got response");
                    return parseVideoData(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ [Consumet] error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    // ========================================
    // Consumet /watch response structure:
    // {
    //   "sources": [{ "url", "quality", "isM3U8" }],
    //   "subtitles": [{ "url", "lang" }],    ← NOTE: Consumet dùng "subtitles" + "lang"
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

        // subtitles -> map sang tracks (Consumet dùng "subtitles", frontend expect "tracks")
        List<VideoTrack> tracks = new ArrayList<>();
        response.path("subtitles").forEach(s -> {
            tracks.add(new VideoTrack(
                    s.path("url").asText(""),
                    s.path("lang").asText(""),   // Consumet dùng "lang"
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