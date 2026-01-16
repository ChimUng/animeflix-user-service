package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.repository.RedisEpisodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EpisodeService {

    private static final Logger log = LoggerFactory.getLogger(EpisodeService.class);

    private final MalSyncClient malSyncClient;
    private final ZoroClient zoroClient;
    private final GogoanimeClient gogoanimeClient;
    private final ConsumetClient consumetClient;
    private final AnifyClient anifyClient;
    private final AniZipClient aniZipClient;
    private final RedisEpisodeRepository redisRepository;
    private final ObjectMapper objectMapper;

    public EpisodeService(
            MalSyncClient malSyncClient,
            ZoroClient zoroClient,
            GogoanimeClient gogoanimeClient,
            ConsumetClient consumetClient,
            AnifyClient anifyClient,
            AniZipClient aniZipClient,
            RedisEpisodeRepository redisRepository,
            ObjectMapper objectMapper
    ) {
        this.malSyncClient = malSyncClient;
        this.zoroClient = zoroClient;
        this.gogoanimeClient = gogoanimeClient;
        this.consumetClient = consumetClient;
        this.anifyClient = anifyClient;
        this.aniZipClient = aniZipClient;
        this.redisRepository = redisRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch episodes từ các provider + AniZip metadata → merge → return EpisodeResponse.
     * Fetch episodes with caching logic.
     */
    public Mono<EpisodeResponse> fetchEpisodes(String animeId, boolean releasing, boolean refresh) {
        log.info("Fetching episode data for anime ID: {} (releasing: {}, refresh: {})", animeId, releasing, refresh);

        long cacheTimeSeconds = releasing ? 3 * 60 * 60 : 45 * 24 * 60 * 60; // 3 hours or 45 days
        String episodeKey = "episode:" + animeId;
        String metaKey = "meta:" + animeId;

        if (refresh) {
            // Force refresh: delete old caches if exist
            return Mono.zip(redisRepository.deleteKey(episodeKey), redisRepository.deleteKey(metaKey))
                    .then(fetchAndCacheData(animeId, cacheTimeSeconds))
                    .onErrorResume(e -> {
                        log.error("Failed to fetch fresh episode data for anime ID: {}", animeId, e);
                        return Mono.error(new EpisodeFetchException("Fetch failed for ID: " + animeId));
                    });
        } else {
            // Check caches
            return Mono.zip(
                            getCachedProviders(episodeKey),
                            getCachedMeta(metaKey)
                    ).flatMap(tuple -> {
                        List<Provider> cachedProviders = tuple.getT1();
                        List<EpisodeMeta> cachedMeta = tuple.getT2();

                        if (!cachedProviders.isEmpty() && !cachedMeta.isEmpty()) {
                            // Both cached: combine and return
                            log.info("Using cached data for anime ID: {}", animeId);
                            combineEpisodeMeta(cachedProviders, cachedMeta);
                            return Mono.just(new EpisodeResponse(cachedProviders));
                        } else {
                            // Missing some: fetch fresh and cache
                            return fetchAndCacheData(animeId, cacheTimeSeconds);
                        }
                    }).switchIfEmpty(Mono.defer(() -> fetchAndCacheData(animeId, cacheTimeSeconds)))
                    .onErrorResume(e -> {
                        log.error("Failed to fetch episode data for anime ID: {}", animeId, e);
                        return Mono.error(new EpisodeFetchException("Fetch failed for ID: " + animeId));
                    });
        }
    }

    private Mono<List<Provider>> getCachedProviders(String key) {
        return redisRepository.getCachedData(key)
                .flatMap(json -> {
                    try {
                        List<Provider> providers = objectMapper.readValue(json, new TypeReference<>() {});
                        return providers != null && !providers.isEmpty() ? Mono.just(providers) : Mono.empty();
                    } catch (Exception e) {
                        log.error("Error deserializing providers from cache: {}", key, e);
                        return redisRepository.deleteKey(key).then(Mono.empty());
                    }
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    private Mono<List<EpisodeMeta>> getCachedMeta(String key) {
        return redisRepository.getCachedData(key)
                .flatMap(json -> {
                    try {
                        List<EpisodeMeta> meta = objectMapper.readValue(json, new TypeReference<>() {});
                        return meta != null && !meta.isEmpty() ? Mono.just(meta) : Mono.empty();
                    } catch (Exception e) {
                        log.error("Error deserializing meta from cache: {}", key, e);
                        return redisRepository.deleteKey(key).then(Mono.empty());
                    }
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    /**
     * Fetch fresh data, combine, cache, and return.
     */
    private Mono<EpisodeResponse> fetchAndCacheData(String animeId, long cacheTimeSeconds) {
        // 1️⃣ Lấy danh sách Provider từ MalSync (để biết các nguồn như zoro/gogo/anify)
        return malSyncClient.fetchMalSync(animeId)
                .flatMap(entries -> {
                    if (entries.length == 0) {
                        // Fallback to Consumet + Anify
                        log.info("No providers from MalSync, falling back to Consumet and Anify for anime ID: {}", animeId);
                        return Mono.zip(
                                consumetClient.fetchConsumet(animeId),
                                anifyClient.fetchAnify(animeId)
                        ).map(tuple -> {
                            List<Provider> combined = new ArrayList<>();
                            combined.add(tuple.getT1());
                            combined.addAll(tuple.getT2());
                            return combined;
                        });
                    } else {
                        return fetchProviderData(entries, animeId);
                    }
                })
                .flatMap(providers -> {
                    if (providers.isEmpty()) {
                        return Mono.error(new EpisodeFetchException("No providers found for ID: " + animeId));
                    }
                    return Mono.zip(
                            Mono.just(providers),
                            aniZipClient.fetchEpisodeMeta(animeId)
                    ).flatMap(tuple -> {
                        List<Provider> providerData = tuple.getT1();
                        List<EpisodeMeta> metaList = tuple.getT2();

                        combineEpisodeMeta(providerData, metaList);

                        // Cache providers and meta separately
                        String episodeKey = "episode:" + animeId;
                        String metaKey = "meta:" + animeId;

                        return Mono.zip(
                                redisRepository.setCachedData(episodeKey, providerData, cacheTimeSeconds),
                                redisRepository.setCachedData(metaKey, metaList, cacheTimeSeconds)
                        ).then(Mono.just(new EpisodeResponse(providerData)));
                    });
                })
                .doOnSuccess(response -> log.info("Successfully fetched and cached episode data for anime ID: {}", animeId))
                .defaultIfEmpty(new EpisodeResponse(Collections.emptyList()));
    }

    /**
     * Fetch episode list cho từng Provider (song song) dựa trên MalSyncEntry.
     */
    private Mono<List<Provider>> fetchProviderData(MalSyncEntry[] entries, String animeId) {
        return Flux.fromArray(entries)
                .flatMap(entry -> fetchSingleProvider(entry, animeId))  // Fetch parallel mỗi entry
                .filter(Objects::nonNull)
                .collectList()
//                .flatMap(list -> {
//                    if (list.isEmpty()) {
//                        log.info("No Zoro or Gogoanime providers found, falling back to Anify for anime ID: {}", animeId);
//                        return anifyClient.fetchAnify(animeId);
//                    } else {
//                        return Mono.just(list);
//                    }
//                }) chuyển logic fallback len hàm fetchandgetcachedata
                .defaultIfEmpty(Collections.emptyList());
    }

    /**
     * Fetch cho một provider duy nhất, extract ID từ URL.
     */
    private Mono<Provider> fetchSingleProvider(MalSyncEntry entry, String animeId) {
        String providerName = entry.getProviderId(); //kiểm trả lại provider của từng đoạn fetchProviderData có dữ liệu data không
        String url = entry.getSub();    // y như trên
        if (url == null || url.isEmpty()) {
            log.warn("No URL for provider: {}, fallback to default ID: {}", providerName, animeId);
            url = generateFallbackUrl(providerName, animeId);  // Helper để tạo URL fallback nếu cần
        }

        String id = extractIdFromUrl(providerName, url);  // Extract ID
        if (id == null) {
            log.error("Failed to extract ID from URL: {} for provider: {}", url, providerName);
            return Mono.empty();  // Skip nếu fail
        }

        return switch (providerName) {
            case "zoro" -> zoroClient.fetchZoro(id);  // Adjust client để nhận ID thay vì URL
            case "gogoanime" -> gogoanimeClient.fetchGogoanime(id);  // Tương tự, có thể cần sub/dub separate
            case "consumet" -> consumetClient.fetchConsumet(id);
//            case "anify" -> anifyClient.fetchAnify(id);
            default -> {
                log.warn("Unknown provider: {}", providerName);
                yield Mono.empty();
            }
        };
    }

    /**
     * Helper: Extract ID từ URL dựa trên provider.
     */
    private String extractIdFromUrl(String providerName, String url) {
        // Logic parse tùy provider (dựa trên docs API)
        return switch (providerName) {
            case "zoro" -> url.substring(url.lastIndexOf('/') + 1);  // e.g., "anime-title-123" từ "https://zoro.to/anime-title-123"
            case "gogoanime" -> url.replace("https://gogoanime.tv/category/", "");  // e.g., "anime-slug"
            // Thêm case cho consumet/anify nếu cần
            default -> null;
        };
    }

    /**
     * Helper: Generate fallback URL/ID nếu MalSync không có.
     */
    private String generateFallbackUrl(String providerName, String animeId) {
        // Ví dụ fallback đơn giản: dùng animeId làm ID
        return switch (providerName) {
            case "zoro" -> "https://zoro.to/" + animeId;  // Adjust theo docs,chỉ cần quan tâm animeId
            case "gogoanime" -> "https://gogoanime.tv/category/" + animeId;
            default -> "";
        };
    }

    /**
     * Merge EpisodeMeta (image, title, summary, etc.) vào từng Episode trong mỗi Provider.
     */
    private void combineEpisodeMeta(List<Provider> providers, List<EpisodeMeta> metaList) {
        Map<String, EpisodeMeta> metaMap = metaList.stream()
                .collect(Collectors.toMap(EpisodeMeta::getEpisode, m -> m));

        for (Provider provider : providers) {
            Object episodesObj = provider.getEpisodes();
            if (episodesObj == null) continue;

            if (episodesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Episode> episodes = (List<Episode>) episodesObj;
                episodes.forEach(episode -> mergeMetaIntoEpisode(episode, metaMap));
            } else if (episodesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, List<Episode>> episodesMap = (Map<String, List<Episode>>) episodesObj;
                episodesMap.values().forEach(episodeList ->
                        episodeList.forEach(episode -> mergeMetaIntoEpisode(episode, metaMap))
                );
            } else {
                log.warn("Unexpected episodes type for provider {}: {}", provider.getId(), episodesObj.getClass());
            }
        }
    }

    /**
      * Merge metadata cho một episode duy nhất (in-place).
     */
    private void mergeMetaIntoEpisode(Episode episode, Map<String, EpisodeMeta> metaMap) {
        EpisodeMeta meta = metaMap.get(String.valueOf(episode.getNumber()));
        if (meta != null) {
            Map<String, String> titleMap = meta.getTitle();
            String newTitle = (titleMap != null && titleMap.containsKey("en"))
                    ? titleMap.get("en")
                    : episode.getTitle();
            episode.setTitle(newTitle);
            episode.setImage(meta.getImage());
            episode.setDescription(meta.getSummary());
        }
    }
}
