package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.util.SlugBuilder;
import com.animeflix.animeepisode.model.stream.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ✅ UPDATED NineAnimeStreamClient - Sử dụng WebClient từ Config & SlugBuilder
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NineAnimeStreamClient {

    // ✅ Inject đúng Bean "nineAnimeWebClient" đã cấu hình trong ExternalApiConfig
    @Qualifier("nineAnimeWebClient")
    private final WebClient webClient;

    private final SlugBuilder slugBuilder;

    /**
     * @param episodeid  episodeId từ provider
     * @param animeId    anilist ID (dùng để build slug nếu cần)
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetch9AnimeStream(String episodeid, String animeId, String subtype) {
        return slugBuilder.buildZoroEpisodeId(animeId, episodeid)
                .flatMap(paramValue -> {
                    log.info("🎯 [9anime] final animeEpisodeId: {}", paramValue);
                    return fetchStream(paramValue, subtype);
                });
    }

    private Mono<VideoData> fetchStream(String animeEpisodeId, String subtype) {
        // ✅ Đã đổi từ "zenimeWebClient" thành "webClient" để khớp với biến khai báo
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/stream")
                        .queryParam("id", animeEpisodeId)
                        .queryParam("server", "hd-2")
                        .queryParam("type", subtype)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    if (!response.path("success").asBoolean(false)) {
                        log.warn("⚠️ [9anime] API returned success=false");
                        return null;
                    }

                    JsonNode streamingLink = response.path("results").path("streamingLink");
                    if (streamingLink.isMissingNode() || streamingLink.isNull()) {
                        log.error("❌ [9anime] No streamingLink");
                        return null;
                    }

                    JsonNode link = streamingLink.path("link");
                    String fileUrl = link.path("file").asText();
                    if (fileUrl.isEmpty()) {
                        log.error("❌ [9anime] No file URL in link");
                        return null;
                    }

                    log.info("✅ [9anime] Got videoData");
                    return parseVideoData(streamingLink, fileUrl, link);
                })
                .onErrorResume(e -> {
                    log.error("❌ [9anime] stream error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private VideoData parseVideoData(JsonNode streamingLink, String fileUrl, JsonNode link) {
        VideoData videoData = new VideoData();

        // sources
        String linkType = link.path("type").asText("");
        videoData.setSources(List.of(new VideoSource(
                fileUrl,
                null,
                "hls".equals(linkType),
                linkType
        )));

        // tracks
        List<VideoTrack> tracks = new ArrayList<>();
        streamingLink.path("tracks").forEach(t -> {
            tracks.add(new VideoTrack(
                    t.path("file").asText(""),
                    t.path("label").asText(""),
                    t.path("kind").asText(""),
                    t.path("default").asBoolean(false)
            ));
        });
        if (!tracks.isEmpty()) videoData.setTracks(tracks);

        // intro/outro
        videoData.setIntro(parseRange(streamingLink.path("intro")));
        videoData.setOutro(parseRange(streamingLink.path("outro")));

        // headers
        videoData.setHeaders(Map.of("Referer", "https://rapid-cloud.co/"));

        return videoData;
    }

    private VideoTimeRange parseRange(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        return new VideoTimeRange(
                node.path("start").asInt(),
                node.path("end").asInt()
        );
    }
}