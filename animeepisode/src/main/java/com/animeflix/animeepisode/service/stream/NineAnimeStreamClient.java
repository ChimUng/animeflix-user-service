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

@Component
@RequiredArgsConstructor
@Slf4j
public class NineAnimeStreamClient {

    private final WebClient nineAnimeWebClient;
    private final SlugBuilder slugBuilder;

    /**
     * ✅ FIXED - Giống Next.js nineAnimeEpisode()
     *
     * @param episodeid  episodeId từ provider:
     *                   - "2142" (số thuần) → cần build slug
     *                   - "one-piece-100?ep=2142" (đã built) → dùng luôn
     * @param animeId    anilist ID (dùng để build slug nếu cần)
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetch9AnimeStream(String episodeid, String animeId, String subtype) {
        // ✅ KIỂM TRA: Nếu episodeid đã chứa "?ep=" thì đã được build rồi
        if (episodeid.contains("?ep=")) {
            log.info("✅ [9anime] episodeid đã ở dạng đầy đủ: {}", episodeid);
            return fetchStream(episodeid, subtype);
        }

        // ✅ Nếu episodeid chỉ là số episode thuần túy, build animeEpisodeId
        log.info("🔨 [9anime] Building animeEpisodeId từ: anilistId={}, episodeId={}", animeId, episodeid);
        return slugBuilder.buildZoroEpisodeId(animeId, episodeid)
                .flatMap(animeEpisodeId -> {
                    // Fallback: nếu vẫn null thì dùng episodeid gốc
                    String paramValue = animeEpisodeId != null ? animeEpisodeId : episodeid;
                    log.info("🎯 [9anime] Final animeEpisodeId: {}", paramValue);
                    return fetchStream(paramValue, subtype);
                });
    }

    private Mono<VideoData> fetchStream(String animeEpisodeId, String subtype) {
        return nineAnimeWebClient.get()
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