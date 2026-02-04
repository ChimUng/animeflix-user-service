package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.model.stream.VideoData;
import com.animeflix.animeepisode.model.stream.VideoSource;
import com.animeflix.animeepisode.model.stream.VideoTrack;
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
 * 🎬 GogoanimeStreamClient - MISSING CLIENT
 *
 * Fetch streams from Gogoanime via Consumet API
 *
 * API: GET ${CONSUMET_URI}/meta/anilist/watch/${episodeId}
 *
 * Used when:
 * - source === "consumet"
 * - provider === "gogoanime" | "gogobackup"
 *
 * NOTE: Giống ConsumetStreamClient nhưng có thể có custom logic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GogoanimeStreamClient {

    private final WebClient consumetWebClient;

    /**
     * Fetch Gogoanime stream từ Consumet API
     *
     * @param episodeId  Episode ID (format: "one-piece-episode-1")
     * @return           VideoData hoặc empty
     */
    public Mono<VideoData> fetchGogoanimeStream(String episodeId) {
        log.info("🔍 [Gogoanime] Fetching stream for episodeId: {}", episodeId);

        return consumetWebClient.get()
                .uri("/meta/anilist/watch/" + episodeId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    // Validate có sources
                    JsonNode sourcesNode = response.path("sources");
                    if (!sourcesNode.isArray() || sourcesNode.isEmpty()) {
                        log.error("❌ [Gogoanime] No sources");
                        return null;
                    }

                    log.info("✅ [Gogoanime] Got response");
                    return parseVideoData(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ [Gogoanime] error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Parse Consumet response → VideoData
     *
     * Consumet Gogoanime response structure:
     * {
     *   "sources": [{ "url", "quality", "isM3U8" }],
     *   "subtitles": [{ "url", "lang" }],
     *   "headers": { "Referer": "..." },
     *   "download": "...",
     *   "intro": { "start", "end" },
     *   "outro": { "start", "end" }
     * }
     */
    private VideoData parseVideoData(JsonNode response) {
        VideoData videoData = new VideoData();

        // ✅ sources
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

        // ✅ subtitles → tracks
        List<VideoTrack> tracks = new ArrayList<>();
        response.path("subtitles").forEach(s -> {
            tracks.add(new VideoTrack(
                    s.path("url").asText(""),
                    s.path("lang").asText(""),
                    "subtitles",
                    false
            ));
        });
        if (!tracks.isEmpty()) {
            videoData.setTracks(tracks);
        }

        // ✅ headers
        Map<String, String> headers = new HashMap<>();
        response.path("headers").fields().forEachRemaining(entry ->
                headers.put(entry.getKey(), entry.getValue().asText(""))
        );
        if (!headers.isEmpty()) {
            videoData.setHeaders(headers);
        }

        // ✅ intro/outro (optional)
        JsonNode intro = response.path("intro");
        if (!intro.isMissingNode() && !intro.isNull()) {
            videoData.setIntro(new com.animeflix.animeepisode.model.stream.VideoTimeRange(
                    intro.path("start").asInt(),
                    intro.path("end").asInt()
            ));
        }

        JsonNode outro = response.path("outro");
        if (!outro.isMissingNode() && !outro.isNull()) {
            videoData.setOutro(new com.animeflix.animeepisode.model.stream.VideoTimeRange(
                    outro.path("start").asInt(),
                    outro.path("end").asInt()
            ));
        }

        return videoData;
    }
}