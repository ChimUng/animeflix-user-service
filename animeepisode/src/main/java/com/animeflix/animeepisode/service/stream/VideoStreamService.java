package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.repository.RedisEpisodeRepository;
import com.animeflix.animeepisode.model.stream.VideoData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 🎬 VideoStreamService - UPDATED với methods riêng cho từng provider
 *
 * ✅ NEW: Provider-specific methods
 * ❌ DEPRECATE: Generic fetchStream() method
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStreamService {

    private static final long CACHE_TIME_SECONDS = 25 * 60; // 25 phút

    private final ConsumetStreamClient consumetClient;
    private final GogoanimeStreamClient gogoanimeClient;
    private final ZoroStreamClient zoroClient;
    private final NineAnimeStreamClient nineAnimeClient;
    private final AnimePaheStreamClient animePaheClient;
    private final AnifyStreamClient anifyClient;

    private final RedisEpisodeRepository redisRepository;
    private final ObjectMapper objectMapper;

    // ========================================
    // 🆕 NEW METHODS - Provider-specific
    // ========================================

    /**
     * Fetch Consumet stream (Gogoanime/Gogobackup)
     */
    public Mono<VideoData> fetchConsumetStream(String episodeId, boolean refresh) {
        log.info("🔍 [Service] Consumet stream: episodeId={}, refresh={}", episodeId, refresh);

        String cacheKey = buildCacheKey("consumet", episodeId);

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCacheConsumet(episodeId, cacheKey));
        }

        return getCachedVideoData(cacheKey)
                .switchIfEmpty(fetchAndCacheConsumet(episodeId, cacheKey));
    }

    /**
     * Fetch Zoro stream
     */
    public Mono<VideoData> fetchZoroStream(
            String anilistId,
            String episodeId,
            String subtype,
            boolean refresh
    ) {
        log.info("🔍 [Service] Zoro stream: anilistId={}, episodeId={}, subtype={}, refresh={}",
                anilistId, episodeId, subtype, refresh);

        String cacheKey = buildCacheKey("zoro", anilistId, episodeId, subtype);

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCacheZoro(anilistId, episodeId, subtype, cacheKey));
        }

        return getCachedVideoData(cacheKey)
                .switchIfEmpty(fetchAndCacheZoro(anilistId, episodeId, subtype, cacheKey));
    }

    /**
     * Fetch 9anime stream
     */
    public Mono<VideoData> fetch9AnimeStream(
            String anilistId,
            String episodeId,
            String subtype,
            boolean refresh
    ) {
        log.info("🔍 [Service] 9anime stream: anilistId={}, episodeId={}, subtype={}, refresh={}",
                anilistId, episodeId, subtype, refresh);

        String cacheKey = buildCacheKey("9anime", anilistId, episodeId, subtype);

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCache9Anime(anilistId, episodeId, subtype, cacheKey));
        }

        return getCachedVideoData(cacheKey)
                .switchIfEmpty(fetchAndCache9Anime(anilistId, episodeId, subtype, cacheKey));
    }

    /**
     * Fetch AnimePahe stream
     */
    public Mono<VideoData> fetchAnimePaheStream(String episodeId, boolean refresh) {
        log.info("🔍 [Service] AnimePahe stream: episodeId={}, refresh={}", episodeId, refresh);

        String cacheKey = buildCacheKey("animepahe", episodeId);

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCacheAnimePahe(episodeId, cacheKey));
        }

        return getCachedVideoData(cacheKey)
                .switchIfEmpty(fetchAndCacheAnimePahe(episodeId, cacheKey));
    }

    /**
     * Fetch Anify stream (generic fallback)
     */
    public Mono<VideoData> fetchAnifyStream(
            String provider,
            String episodeId,
            String episodeNum,
            String anilistId,
            String subtype,
            boolean refresh
    ) {
        log.info("🔍 [Service] Anify stream: provider={}, episodeId={}, refresh={}",
                provider, episodeId, refresh);

        String cacheKey = buildCacheKey("anify", provider, anilistId, episodeId, subtype);

        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCacheAnify(provider, episodeId, episodeNum, anilistId, subtype, cacheKey));
        }

        return getCachedVideoData(cacheKey)
                .switchIfEmpty(fetchAndCacheAnify(provider, episodeId, episodeNum, anilistId, subtype, cacheKey));
    }

    // ========================================
    // FETCH & CACHE HELPERS
    // ========================================

    private Mono<VideoData> fetchAndCacheConsumet(String episodeId, String cacheKey) {
        return consumetClient.fetchConsumetStream(episodeId)
                .flatMap(videoData -> cacheIfValid(videoData, cacheKey));
    }

    private Mono<VideoData> fetchAndCacheZoro(
            String anilistId,
            String episodeId,
            String subtype,
            String cacheKey
    ) {
        return zoroClient.fetchZoroStream(episodeId, anilistId, subtype)
                .flatMap(videoData -> cacheIfValid(videoData, cacheKey));
    }

    private Mono<VideoData> fetchAndCache9Anime(
            String anilistId,
            String episodeId,
            String subtype,
            String cacheKey
    ) {
        return nineAnimeClient.fetch9AnimeStream(episodeId, anilistId, subtype)
                .flatMap(videoData -> cacheIfValid(videoData, cacheKey));
    }

    private Mono<VideoData> fetchAndCacheAnimePahe(String episodeId, String cacheKey) {
        return animePaheClient.fetchAnimePaheStream(episodeId)
                .flatMap(videoData -> cacheIfValid(videoData, cacheKey));
    }

    private Mono<VideoData> fetchAndCacheAnify(
            String provider,
            String episodeId,
            String episodeNum,
            String anilistId,
            String subtype,
            String cacheKey
    ) {
        return anifyClient.fetchAnifyStream(provider, episodeId, episodeNum, anilistId, subtype)
                .flatMap(videoData -> cacheIfValid(videoData, cacheKey));
    }

    // ========================================
    // CACHE UTILITIES
    // ========================================

    /**
     * Cache VideoData nếu có sources
     */
    private Mono<VideoData> cacheIfValid(VideoData videoData, String cacheKey) {
        if (videoData == null || videoData.getSources() == null || videoData.getSources().isEmpty()) {
            log.warn("⚠️ Not caching: empty sources");
            return videoData != null ? Mono.just(videoData) : Mono.empty();
        }

        return redisRepository.setCachedData(cacheKey, videoData, CACHE_TIME_SECONDS)
                .thenReturn(videoData)
                .onErrorResume(e -> {
                    log.error("❌ Cache error: {}", e.getMessage());
                    return Mono.just(videoData); // Return data anyway
                });
    }

    /**
     * Get cached VideoData
     */
    private Mono<VideoData> getCachedVideoData(String cacheKey) {
        return redisRepository.getCachedData(cacheKey)
                .flatMap(json -> {
                    try {
                        VideoData data = objectMapper.readValue(json, VideoData.class);
                        log.info("✅ Cache hit: {}", cacheKey);
                        return Mono.just(data);
                    } catch (Exception e) {
                        log.error("❌ Cache deserialize error: {}", e.getMessage());
                        return redisRepository.deleteKey(cacheKey).then(Mono.empty());
                    }
                });
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(String... parts) {
        return "stream:" + String.join(":", parts);
    }
}