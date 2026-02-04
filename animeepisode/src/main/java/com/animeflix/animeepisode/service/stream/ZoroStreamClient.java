package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.util.SlugBuilder;
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
    private final SlugBuilder slugBuilder;

    /**
     * ✅ FIXED - Entry point giống Next.js zoroEpisode()
     *
     * @param episodeid  episodeId từ provider:
     *                   - "2142" (số thuần) → cần build slug
     *                   - "one-piece-100?ep=2142" (đã built) → dùng luôn
     * @param animeId    anilist ID của anime (dùng để build slug nếu cần)
     * @param subtype    "sub" | "dub"
     */
    public Mono<VideoData> fetchZoroStream(String episodeid, String animeId, String subtype) {
        // ✅ KIỂM TRA: Nếu episodeid đã chứa "?ep=" thì đã được build rồi
        if (episodeid.contains("?ep=")) {
            log.info("✅ Zoro: episodeid đã ở dạng đầy đủ: {}", episodeid);
            return fetchServersAndStream(episodeid, subtype);
        }

        // ✅ Nếu episodeid chỉ là số episode thuần túy, build animeEpisodeId
        log.info("🔨 Zoro: Building animeEpisodeId từ: anilistId={}, episodeId={}", animeId, episodeid);
        return slugBuilder.buildZoroEpisodeId(animeId, episodeid)
                .flatMap(animeEpisodeId -> {
                    // Fallback: nếu vẫn null thì dùng episodeid gốc
                    String paramValue = animeEpisodeId != null ? animeEpisodeId : episodeid;
                    log.info("🎯 Zoro final animeEpisodeId: {}", paramValue);
                    return fetchServersAndStream(paramValue, subtype);
                });
    }

    private Mono<VideoData> fetchServersAndStream(String animeEpisodeId, String subtype) {
        log.info("🔍 Zoro: Fetching servers for episodeId: {}, subtype: {}", animeEpisodeId, subtype);

        return zoroWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/episode/servers")
                        .queryParam("animeEpisodeId", animeEpisodeId)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .doOnNext(resp -> log.debug("🔍 Zoro servers response: {}", resp))
                .flatMap(serverResponse -> {
                    // ✅ Check status code first
                    int status = serverResponse.path("status").asInt(0);
                    if (status != 200) {
                        log.error("❌ Zoro: Server API returned status: {}", status);
                        return Mono.empty();
                    }

                    JsonNode serverData = serverResponse.path("data");
                    if (serverData.isMissingNode() || serverData.isNull()) {
                        log.error("❌ Zoro: No serverData in response");
                        return Mono.empty();
                    }

                    JsonNode serverList = serverData.path(subtype); // "sub" or "dub"
                    if (!serverList.isArray() || serverList.isEmpty()) {
                        log.error("❌ Zoro: No serverList cho subtype: {}", subtype);
                        return Mono.empty();
                    }

                    // ✅ Prefer hd-2 server (index 1), fallback to first available
                    JsonNode firstServer = serverList.size() > 1
                            ? serverList.get(1)
                            : serverList.get(0);

                    String serverName = firstServer.path("serverName").asText("");
                    if (serverName.isEmpty()) {
                        log.error("❌ Zoro: Empty serverName");
                        return Mono.empty();
                    }

                    log.info("🎬 Zoro using server: {}", serverName);

                    // ✅ Fetch sources
                    return zoroWebClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/episode/sources")
                                    .queryParam("animeEpisodeId", animeEpisodeId)
                                    .queryParam("server", serverName)
                                    .queryParam("category", subtype)
                                    .build())
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .timeout(Duration.ofSeconds(15))
                            .doOnNext(resp -> log.debug("🔍 Zoro sources response: {}", resp));
                })
                .map(sourceResponse -> {
                    // ✅ Check status
                    int status = sourceResponse.path("status").asInt(0);
                    if (status != 200) {
                        log.error("❌ Zoro: Sources API returned status: {}", status);
                        return null;
                    }

                    JsonNode videoData = sourceResponse.path("data");
                    if (videoData.isMissingNode() || videoData.isNull()) {
                        log.error("❌ Zoro: No videoData in source response");
                        return null;
                    }

                    // ✅ Validate sources exist
                    JsonNode sources = videoData.path("sources");
                    if (!sources.isArray() || sources.isEmpty()) {
                        log.error("❌ Zoro: No sources in videoData");
                        return null;
                    }

                    log.info("✅ Zoro: Got videoData with {} sources", sources.size());
                    return parseVideoData(videoData);
                })
                .onErrorResume(e -> {
                    log.error("❌ Zoro stream error: {}", e.getMessage(), e);
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
            source.setIsM3U8(s.path("isM3U8").asBoolean(false));
            source.setType(source.getIsM3U8() ? "hls" : "mp4");
            sources.add(source);
        });
        videoData.setSources(sources);

        // tracks (subtitles)
        List<VideoTrack> tracks = new ArrayList<>();
        node.path("tracks").forEach(t -> {
            VideoTrack track = new VideoTrack();
            track.setUrl(t.path("url").asText(""));
            track.setLang(t.path("lang").asText(""));
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