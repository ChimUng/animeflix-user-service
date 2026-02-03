package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.service.*;
import com.animeflix.animeepisode.model.stream.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 9anime stream client.
 * Flow giống Next.js nineAnimeEpisode():
 *   1. Build animeEpisodeId (giống Zoro: slug?ep=X)
 *   2. GET ${ZENIME_URL}/api/stream?id=...&server=hd-2&type=${subtype}
 *   3. Parse streamingLink -> VideoData
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NineAnimeStreamClient {

    private final WebClient zenimeWebClient;
    private final MalSyncClient malSyncClient;
    private final AniZipClient aniZipClient;

    /**
     * @param episodeid  episodeId từ provider
     * @param animeId    anilist ID (dùng để build slug nếu cần)
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetch9AnimeStream(String episodeid, String animeId, String subtype) {
        return buildAnimeEpisodeId(animeId, episodeid)
                .flatMap(paramValue -> {
                    log.info("🎯 [9anime] final animeEpisodeId: {}", paramValue);
                    return fetchStream(paramValue, subtype);
                });
    }

    // ========================================
    // Build animeEpisodeId — SAME logic as ZoroStreamClient
    // (reuse via shared clients, không duplicate)
    // ========================================
    private Mono<String> buildAnimeEpisodeId(String animeId, String episodeid) {
        if (episodeid.contains("?ep=")) {
            log.info("✅ [9anime] episodeid đã ở dạng đầy đủ: {}", episodeid);
            return Mono.just(episodeid);
        }

        log.info("🔨 [9anime] building animeEpisodeId từ animeId={}, episodeid={}", animeId, episodeid);

        return malSyncClient.getZoroSlug(animeId)
                .map(map -> (String) map.get("slug"))
                .flatMap(slug -> {
                    if (slug != null && !slug.isEmpty()) {
                        return Mono.just(slug + "?ep=" + episodeid);
                    }
                    return aniZipClient.fetchMalIdFromAnilist(animeId)
                            .flatMap(malId -> {
                                if (malId == null || malId.equals(animeId)) {
                                    return Mono.just(episodeid);
                                }
                                return malSyncClient.getZoroSlug(malId)
                                        .map(map2 -> {
                                            String slug2 = (String) map2.get("slug");
                                            return (slug2 != null && !slug2.isEmpty())
                                                    ? slug2 + "?ep=" + episodeid
                                                    : episodeid;
                                        });
                            })
                            .defaultIfEmpty(episodeid);
                })
                .defaultIfEmpty(episodeid);
    }

    // ========================================
    // GET /api/stream → parse streamingLink → VideoData
    // Matches Next.js nineAnimeEpisode() response mapping
    // ========================================
    private Mono<VideoData> fetchStream(String animeEpisodeId, String subtype) {
        return zenimeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/stream")
                        .queryParam("id", animeEpisodeId)
                        .queryParam("server", "hd-2")   // ✅ default "hd-2" giống Next.js
                        .queryParam("type", subtype)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .map(response -> {
                    // Validate success
                    if (!response.path("success").asBoolean(false)) {
                        log.warn("⚠️ [9anime] API returned success=false");
                        return null;
                    }

                    JsonNode streamingLink = response.path("results").path("streamingLink");
                    if (streamingLink.isMissingNode()) {
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

    // ========================================
    // Map Zenime streamingLink response -> VideoData
    // Giống Next.js mapping:
    //   sources: [{ url: link.file, isM3U8: link.type === 'hls' }]
    //   tracks:  streamingLink.tracks -> { url, lang, kind, default }
    //   intro/outro: streamingLink.intro / outro
    //   headers: { Referer: 'https://rapid-cloud.co/' }
    // ========================================
    private VideoData parseVideoData(JsonNode streamingLink, String fileUrl, JsonNode link) {
        VideoData videoData = new VideoData();

        // sources
        String linkType = link.path("type").asText("");
        videoData.setSources(List.of(new VideoSource(
                fileUrl,
                null,                           // quality: Zenime không trả
                "hls".equals(linkType),         // isM3U8
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

        // intro
        JsonNode intro = streamingLink.path("intro");
        if (!intro.isMissingNode() && !intro.isNull()) {
            videoData.setIntro(new VideoTimeRange(
                    intro.path("start").asInt(),
                    intro.path("end").asInt()
            ));
        }

        // outro
        JsonNode outro = streamingLink.path("outro");
        if (!outro.isMissingNode() && !outro.isNull()) {
            videoData.setOutro(new VideoTimeRange(
                    outro.path("start").asInt(),
                    outro.path("end").asInt()
            ));
        }

        // headers — ✅ fixed Referer giống Next.js
        videoData.setHeaders(Map.of("Referer", "https://rapid-cloud.co/"));

        return videoData;
    }
}