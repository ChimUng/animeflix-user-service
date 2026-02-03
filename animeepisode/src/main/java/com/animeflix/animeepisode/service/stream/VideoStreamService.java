package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.repository.RedisEpisodeRepository;
import com.animeflix.animeepisode.model.stream.StreamRequest;
import com.animeflix.animeepisode.model.stream.VideoData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Stream orchestrator.
 *
 * Routing priority = Next.js route.tsx POST handler:
 *   1. source === "consumet"    → ConsumetStreamClient
 *   2. provider === "zoro"      → ZoroStreamClient
 *   3. provider === "9anime"    → NineAnimeStreamClient
 *   4. provider === "animepahe" → AnimePaheStreamClient
 *   5. source === "anify"       → AnifyStreamClient
 *   6. default                  → error 400
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStreamService {

    private static final long CACHE_TIME_SECONDS = 25 * 60; // 25 phút giống Next.js commented cache

    private final ConsumetStreamClient consumetClient;
    private final ZoroStreamClient zoroClient;
    private final NineAnimeStreamClient nineAnimeClient;
    private final AnimePaheStreamClient animePaheClient;
    private final AnifyStreamClient anifyClient;
    private final RedisEpisodeRepository redisRepository;
    private final ObjectMapper objectMapper;

    /**
     * Main entry point từ VideoStreamController.
     *
     * @param animeId anilist ID từ URL path
     * @param request POST body
     * @param refresh force refresh cache
     */
    public Mono<VideoData> fetchStream(String animeId, StreamRequest request, boolean refresh) {
        log.info("📥 fetchStream: animeId={}, request={}, refresh={}", animeId, request, refresh);
        validateRequest(request);

        String cacheKey = buildCacheKey(animeId, request.getEpisodeid());

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCache(animeId, request, cacheKey));
        }

        // Try cache first
        return redisRepository.getCachedData(cacheKey)
                .flatMap(this::deserializeCache)
                .switchIfEmpty(fetchAndCache(animeId, request, cacheKey));
    }

    // ========================================
    // Route to correct provider — ORDER matters, matches Next.js
    // ========================================
    private Mono<VideoData> routeToProvider(String animeId, StreamRequest request) {
        String source   = request.getSource();
        String provider = request.getProvider();
        String episodeid    = request.getEpisodeid();
        String episodenum   = request.getEpisodenum();
        String subtype  = request.getSubtype() != null ? request.getSubtype() : "sub";

        // 1. source === "consumet" (gogoanime / gogobackup đi qua đây)
        if ("consumet".equals(source)) {
            log.info("🎯 Routing → Consumet");
            return consumetClient.fetchConsumetStream(episodeid);
        }

        // 2. provider === "zoro"
        if ("zoro".equals(provider)) {
            log.info("🎯 Routing → Zoro");
            return zoroClient.fetchZoroStream(episodeid, animeId, subtype);
        }

        // 3. provider === "9anime"
        if ("9anime".equals(provider)) {
            log.info("🎯 Routing → 9anime");
            return nineAnimeClient.fetch9AnimeStream(episodeid, animeId, subtype);
        }

        // 4. provider === "animepahe"
        if ("animepahe".equals(provider)) {
            log.info("🎯 Routing → AnimePahe");
            return animePaheClient.fetchAnimePaheStream(episodeid);
        }

        // 5. source === "anify" (catch-all cho các provider còn lại: zoro via anify, etc.)
        if ("anify".equals(source)) {
            log.info("🎯 Routing → Anify (provider={})", provider);
            return anifyClient.fetchAnifyStream(provider, episodeid, episodenum, animeId, subtype);
        }

        // 6. No match
        log.warn("⚠️ No matching route: source={}, provider={}", source, provider);
        return Mono.error(new EpisodeFetchException("Invalid source/provider: " + source + "/" + provider));
    }

    // ========================================
    // Cache layer
    // ========================================
    private Mono<VideoData> fetchAndCache(String animeId, StreamRequest request, String cacheKey) {
        return routeToProvider(animeId, request)
                .flatMap(videoData -> {
                    // Chỉ cache nếu có sources
                    if (videoData.getSources() == null || videoData.getSources().isEmpty()) {
                        log.warn("⚠️ Not caching: empty sources");
                        return Mono.just(videoData);
                    }
                    return redisRepository.setCachedData(cacheKey, videoData, CACHE_TIME_SECONDS)
                            .thenReturn(videoData);
                })
                .onErrorResume(e -> {
                    log.error("❌ fetchAndCache error: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<VideoData> deserializeCache(String json) {
        try {
            VideoData data = objectMapper.readValue(json, VideoData.class);
            log.info("✅ Cache hit");
            return Mono.just(data);
        } catch (Exception e) {
            log.error("❌ Cache deserialize error: {}", e.getMessage());
            return Mono.empty(); // fallback to fetch
        }
    }

    // ========================================
    // Helpers
    // ========================================
    private void validateRequest(StreamRequest request) {
        if (request.getSource() == null && request.getProvider() == null) {
            throw new EpisodeFetchException("Missing source and provider in request");
        }
        if (request.getEpisodeid() == null || request.getEpisodeid().isEmpty()) {
            throw new EpisodeFetchException("Missing episodeid in request");
        }
    }

    private String buildCacheKey(String animeId, String episodeid) {
        return "stream:" + animeId + ":" + episodeid;
    }
}