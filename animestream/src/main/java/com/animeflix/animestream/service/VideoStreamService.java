package com.animeflix.animestream.service;

import com.animeflix.animestream.exception.StreamFetchException;
import com.animeflix.animestream.Model.StreamRequest;
import com.animeflix.animestream.Model.StreamResponse;
import com.animeflix.animestream.Repository.RedisVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class VideoStreamService {

    private static final Logger log = LoggerFactory.getLogger(VideoStreamService.class);
    private static final long CACHE_TIME_SECONDS = 25 * 60; // 25 minutes

    private final ConsumetClient consumetClient;
    private final ZoroClient zoroClient;
    private final AnifyClient anifyClient;
    private final MalSyncClient malSyncClient;
    private final AniZipClient aniZipClient;
    private final RedisVideoRepository redisRepository;
    private final ObjectMapper objectMapper;

    public VideoStreamService(
            ConsumetClient consumetClient,
            ZoroClient zoroClient,
            AnifyClient anifyClient,
            MalSyncClient malSyncClient,
            AniZipClient aniZipClient,
            RedisVideoRepository redisRepository,
            ObjectMapper objectMapper) {
        this.consumetClient = consumetClient;
        this.zoroClient = zoroClient;
        this.anifyClient = anifyClient;
        this.malSyncClient = malSyncClient;
        this.aniZipClient = aniZipClient;
        this.redisRepository = redisRepository;
        this.objectMapper = objectMapper;
    }

    public Mono<StreamResponse> fetchStream(String id, StreamRequest request, boolean refresh) {
        log.info("Fetching stream for ID: {} with request: {}, refresh: {}", id, request, refresh);
        validateRequest(request);

        String cacheKey = buildCacheKey(id, request.getEpisodeId());
        if (refresh) {
            return redisRepository.deleteKey(cacheKey)
                    .then(fetchAndCacheStream(id, request, cacheKey));
        }
        return redisRepository.getCachedData(cacheKey)
                .flatMap(this::deserializeCachedData)
                .switchIfEmpty(Mono.defer(() -> fetchAndCacheStream(id, request, cacheKey)));
    }

    private void validateRequest(StreamRequest request) {
        if (request.getSource() == null || request.getProvider() == null || request.getEpisodeId() == null) {
            log.warn("Invalid stream request: {}", request);
            throw new StreamFetchException("Missing required fields in request");
        }
    }

    private String buildCacheKey(String id, String episodeId) {
        return "stream:" + id + ":" + episodeId;
    }

    private Mono<StreamResponse> deserializeCachedData(String json) {
        try {
            StreamResponse response = objectMapper.readValue(json, StreamResponse.class);
            log.info("Retrieved stream data from cache: {}", response);
            return response != null ? Mono.just(response) : Mono.empty();
        } catch (Exception e) {
            log.error("Error deserializing cached stream data: {}", json, e);
            return redisRepository.deleteKey(json)
                    .then(Mono.error(new StreamFetchException("Failed to deserialize cached stream data", e)));
        }
    }

    private Mono<StreamResponse> fetchAndCacheStream(String id, StreamRequest request, String cacheKey) {
        return fetchStreamBySource(id, request)
                .flatMap(response -> cacheResponse(cacheKey, response))
                .doOnSuccess(response -> log.info("Successfully fetched and cached stream for ID: {}", id))
                .onErrorMap(e -> new StreamFetchException("Failed to fetch stream for ID: " + id, e));
    }

    private Mono<StreamResponse> fetchStreamBySource(String id, StreamRequest request) {
        String source = request.getSource();
        String provider = request.getProvider();
        String episodeId = request.getEpisodeId();
        int episodeNum = request.getEpisodeNum();
        String subtype = request.getSubtype() != null ? request.getSubtype() : "sub";

        log.info("Fetching stream for source: {}, provider: {}, episodeId: {}, episodeNum: {}, subtype: {}",
                source, provider, episodeId, episodeNum, subtype);

        switch (source) {
            case "consumet":
                return consumetClient.fetchConsumetEpisode(episodeId, id);
            case "zoro":
                return fetchZoroStream(id, provider, episodeId, episodeNum, subtype);
            case "anify":
                return fetchAnifyEpisodeWithFallback(provider, episodeId, episodeNum, id, subtype);
            default:
                return Mono.error(new StreamFetchException("Invalid source: " + source));
        }
    }

    private Mono<StreamResponse> fetchZoroStream(String id, String provider, String episodeId, int episodeNum, String subtype) {
        return malSyncClient.getZoroSlug(id)
                .switchIfEmpty(fetchFallbackZoroSlug(id))
                .flatMap(slug -> {
                    if (slug == null) {
                        log.warn("No Zoro slug found for ID: {}", id);
                        return fetchAnifyEpisodeWithFallback(provider, episodeId, episodeNum, id, subtype);
                    }
                    log.info("Using Zoro slug: {} for ID: {}", slug, id);
                    return zoroClient.fetchZoroEpisode(provider, episodeId, episodeNum, slug.toString(), subtype)
                            .onErrorResume(e -> {
                                log.error("Zoro fetch failed for ID: {}, falling back to Anify", id, e);
                                return fetchAnifyEpisodeWithFallback(provider, episodeId, episodeNum, id, subtype);
                            });
                });
    }

    private Mono<? extends Map<String, Object>> fetchFallbackZoroSlug(String id) {
        return aniZipClient.fetchMalIdFromAnilist(id)
                .flatMap(malId -> {
                    if (malId == null) {
                        log.warn("No MAL ID found for Anilist ID: {}", id);
                        return Mono.just(null);
                    }
                    log.info("Using AniZip MAL ID: {} for Anilist ID: {}", malId, id);
                    return malSyncClient.getZoroSlug(malId)
                            .map(slug -> {
                                if (slug == null) {
                                    log.warn("No Zoro slug found for MAL ID: {}", malId);
                                }
                                return slug;
                            });
                })
                .defaultIfEmpty(null);
    }

    private Mono<StreamResponse> fetchAnifyEpisodeWithFallback(String provider, String episodeId, int episodeNum, String id, String subtype) {
        return anifyClient.fetchAnifyEpisode(provider, episodeId, episodeNum, id, subtype)
                .onErrorResume(e -> {
                    log.error("Anify fetch failed for ID: {}, no further fallback", id, e);
                    return Mono.just(new StreamResponse(false, null));
                });
    }

    private Mono<StreamResponse> cacheResponse(String cacheKey, StreamResponse response) {
        if (!response.isSuccess() || response.getData() == null) {
            log.warn("Not caching failed or empty response for key: {}", cacheKey);
            return Mono.just(response);
        }
        return redisRepository.setCachedData(cacheKey, response, CACHE_TIME_SECONDS)
                .thenReturn(response)
                .onErrorResume(e -> {
                    log.error("Failed to cache stream data for key: {}", cacheKey, e);
                    return Mono.just(response);
                });
    }
}