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
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnimePaheStreamClient {

    private final WebClient consumetWebClient;

    /**
     * @param episodeid  full episode ID từ AnimePahe (UUID/hash format)
     */
    public Mono<VideoData> fetchAnimePaheStream(String episodeid) {
        log.info("🔍 [AnimePahe] episodeId: {}", episodeid);

        return consumetWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/animepahe/watch")
                        .queryParam("episodeId", episodeid)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    JsonNode sourcesNode = response.path("sources");
                    if (!sourcesNode.isArray() || sourcesNode.isEmpty()) {
                        log.error("❌ [AnimePahe] No sources");
                        return null;
                    }

                    log.info("✅ [AnimePahe] Success");
                    return parseVideoData(response);
                })
                .onErrorResume(e -> {
                    log.error("❌ [AnimePahe] error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    // ========================================
    // Parse Consumet animepahe response -> VideoData
    // API trả sources, tracks, intro, outro — chỉ cần add headers
    // ========================================
    private VideoData parseVideoData(JsonNode response) {
        VideoData videoData = new VideoData();

        // sources
        List<VideoSource> sources = new ArrayList<>();
        response.path("sources").forEach(s -> {
            sources.add(new VideoSource(
                    s.path("url").asText(""),
                    s.path("isM3U8").asBoolean(false),
                    null
            ));
        });
        videoData.setSources(sources);

        // tracks (nếu có)
        List<VideoTrack> tracks = new ArrayList<>();
        response.path("tracks").forEach(t -> {
            tracks.add(new VideoTrack(
                    t.path("file").asText(""),
                    t.path("label").asText(""),
                    t.path("kind").asText(""),
                    t.path("default").asBoolean(false)
            ));
        });
        if (!tracks.isEmpty()) videoData.setTracks(tracks);

        // intro / outro
        JsonNode intro = response.path("intro");
        if (!intro.isMissingNode() && !intro.isNull()) {
            videoData.setIntro(new VideoTimeRange(
                    intro.path("start").asInt(),
                    intro.path("end").asInt()
            ));
        }
        JsonNode outro = response.path("outro");
        if (!outro.isMissingNode() && !outro.isNull()) {
            videoData.setOutro(new VideoTimeRange(
                    outro.path("start").asInt(),
                    outro.path("end").asInt()
            ));
        }

        // ✅ headers — giống Next.js: Referer + Origin
        videoData.setHeaders(Map.of(
                "Referer", "https://kwik.cx/",
                "Origin", "https://animepahe.si"
        ));

        return videoData;
    }
}