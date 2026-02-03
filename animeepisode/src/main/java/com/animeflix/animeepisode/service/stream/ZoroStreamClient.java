package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.service.*;
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
public class ZoroStreamClient {

    private final WebClient zoroWebClient;
    private final MalSyncClient malSyncClient;
    private final AniZipClient aniZipClient;

    /**
     * Entry point — matches Next.js zoroEpisode()
     *
     * @param episodeid  episodeId từ provider (có thể là số thuần hoặc đã có "?ep=")
     * @param animeId    anilist ID của anime (dùng để build slug)
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetchZoroStream(String episodeid, String animeId, String subtype) {
        return buildAnimeEpisodeId(animeId, episodeid)
                .flatMap(paramValue -> {
                    log.info("🎯 Zoro final animeEpisodeId: {}", paramValue);
                    return fetchServersAndStream(paramValue, subtype);
                });
    }

    // ========================================
    // Step 0: Build animeEpisodeId = "${slug}?ep=${episodeid}"
    // Logic 1:1 với Next.js buildZoroAnimeEpisodeId()
    // ========================================
    private Mono<String> buildAnimeEpisodeId(String animeId, String episodeid) {
        // Nếu đã có "?ep=" → đã build rồi, skip
        if (episodeid.contains("?ep=")) {
            log.info("✅ Zoro episodeid đã ở dạng đầy đủ: {}", episodeid);
            return Mono.just(episodeid);
        }

        log.info("🔨 Zoro building animeEpisodeId từ animeId={}, episodeid={}", animeId, episodeid);

        // Try 1: MalSync(anilistId) -> slug
        return malSyncClient.getZoroSlug(animeId)
                .map(map -> (String) map.get("slug"))
                .flatMap(slug -> {
                    if (slug != null && !slug.isEmpty()) {
                        return Mono.just(slug + "?ep=" + episodeid);
                    }

                    // Try 2: AniZip -> malId -> MalSync(malId) -> slug
                    return aniZipClient.fetchMalIdFromAnilist(animeId)
                            .flatMap(malId -> {
                                if (malId == null || malId.equals(animeId)) {
                                    log.warn("⚠️ Zoro: No MAL ID fallback for {}", animeId);
                                    return Mono.just(episodeid); // fallback: dùng episodeid gốc
                                }
                                return malSyncClient.getZoroSlug(malId)
                                        .map(map2 -> {
                                            String slug2 = (String) map2.get("slug");
                                            if (slug2 != null && !slug2.isEmpty()) {
                                                return slug2 + "?ep=" + episodeid;
                                            }
                                            return episodeid; // fallback
                                        });
                            })
                            .defaultIfEmpty(episodeid);
                })
                .defaultIfEmpty(episodeid);
    }

    // ========================================
    // Step 1: GET /episode/servers → pick server[1]
    // Step 2: GET /episode/sources → VideoData
    // ========================================
    private Mono<VideoData> fetchServersAndStream(String animeEpisodeId, String subtype) {
        // Step 1: fetch server list
        return zoroWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/episode/servers")
                        .queryParam("animeEpisodeId", animeEpisodeId)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .flatMap(serverResponse -> {
                    JsonNode serverData = serverResponse.path("data");
                    if (serverData.isMissingNode()) {
                        log.error("❌ Zoro: No serverData");
                        return Mono.empty();
                    }

                    JsonNode serverList = serverData.path(subtype); // "sub" or "dub"
                    if (!serverList.isArray() || serverList.isEmpty()) {
                        log.error("❌ Zoro: No serverList cho subtype: {}", subtype);
                        return Mono.empty();
                    }

                    // ✅ Pick index 1 (giống Next.js: serverList[1])
                    JsonNode firstServer = serverList.size() > 1
                            ? serverList.get(1)
                            : serverList.get(0); // fallback index 0 nếu chỉ có 1

                    String serverName = firstServer.path("serverName").asText();
                    log.info("🎬 Zoro using server: {}", serverName);

                    // Step 2: fetch stream sources
                    return zoroWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/episode/sources")
                                    .queryParam("animeEpisodeId", animeEpisodeId)
                                    .queryParam("server", serverName)
                                    .queryParam("category", subtype)
                                    .build())
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .timeout(Duration.ofSeconds(15));
                })
                .map(sourceResponse -> {
                    JsonNode videoData = sourceResponse.path("data");
                    if (videoData.isMissingNode()) {
                        log.error("❌ Zoro: No videoData in source response");
                        return null;
                    }
                    log.info("✅ Zoro: Got videoData");
                    return parseVideoData(videoData);
                })
                .onErrorResume(e -> {
                    log.error("❌ Zoro stream error: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    // ========================================
    // Parse Zoro source response -> VideoData
    // ========================================
    private VideoData parseVideoData(JsonNode node) {
        VideoData videoData = new VideoData();

        // sources
        List<VideoSource> sources = new ArrayList<>();
        node.path("sources").forEach(s -> {
            VideoSource source = new VideoSource();
            source.setUrl(s.path("url").asText());
            source.setQuality(s.path("quality").asText());
            source.setIsM3U8(s.path("isM3U8").asBoolean(false));
            sources.add(source);
        });
        videoData.setSources(sources);

        // tracks (subtitles)
        List<VideoTrack> tracks = new ArrayList<>();
        node.path("tracks").forEach(t -> {
            VideoTrack track = new VideoTrack();
            track.setUrl(t.path("file").asText(""));
            track.setLang(t.path("label").asText(""));
            track.setKind(t.path("kind").asText(""));
            track.setIsDefault(t.path("default").asBoolean(false));
            tracks.add(track);
        });
        if (!tracks.isEmpty()) videoData.setTracks(tracks);

        // intro / outro
        if (!node.path("intro").isMissingNode()) {
            videoData.setIntro(new VideoTimeRange(
                    node.path("intro").path("start").asInt(),
                    node.path("intro").path("end").asInt()
            ));
        }
        if (!node.path("outro").isMissingNode()) {
            videoData.setOutro(new VideoTimeRange(
                    node.path("outro").path("start").asInt(),
                    node.path("outro").path("end").asInt()
            ));
        }

        // headers
        if (!node.path("headers").isMissingNode()) {
            videoData.setHeaders(Map.of(
                    "Referer", node.path("headers").path("Referer").asText("")
            ));
        }

        return videoData;
    }
}
