package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.mapper.EpisodeMapper;
import com.animeflix.animeepisode.model.*;
import com.animeflix.animeepisode.repository.RedisEpisodeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpisodeService {

    private final MalSyncClient malSyncClient;
    private final ZoroClient zoroClient;
    private final GogoanimeClient gogoanimeClient;
    private final ConsumetClient consumetClient;
    private final AnifyClient anifyClient;
    private final AnimePaheClient animePaheClient;
    private final NineAnimeClient nineAnimeClient;  // ← THÊM MỚI
    private final AniZipClient aniZipClient;
    private final RedisEpisodeRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final EpisodeMapper episodeMapper;

    /**
     * ✅ MAIN FETCH METHOD - (Hàm xử lý cache - fetch nếu cache hit miss hoặc force)
     */
    public Mono<List<Provider>> fetchEpisodes(String animeId, boolean releasing, boolean refresh) {
        log.info("📥 Fetching episodes for anime ID: {} (releasing: {}, refresh: {})", animeId, releasing, refresh);

        long cacheTimeSeconds = releasing ? 3 * 60 * 60 : 45 * 24 * 60 * 60;
        String episodeKey = "episode:" + animeId;
        String metaKey = "meta:" + animeId;

        if (refresh) {
            log.info("🔄 Force refresh requested");
            return Mono.zip(redisRepository.deleteKey(episodeKey), redisRepository.deleteKey(metaKey)).then(fetchAndCacheData(animeId, cacheTimeSeconds));
        }

        return Mono.zip(getCachedProviders(episodeKey), getCachedMeta(metaKey)).flatMap(tuple -> {
            List<Provider> cachedProviders = tuple.getT1();
            List<EpisodeMeta> cachedMeta = tuple.getT2();

            if (!cachedProviders.isEmpty() && !cachedMeta.isEmpty()) {
                log.info("✅ Cache hit for anime ID: {}", animeId);
                combineEpisodeMeta(cachedProviders, cachedMeta);
                return Mono.just(cachedProviders);
            }

            log.debug("⚠️ Cache miss - fetching fresh data");
            return fetchAndCacheData(animeId, cacheTimeSeconds);
        }).switchIfEmpty(
                fetchAndCacheData(animeId, cacheTimeSeconds)
        );
    }

    /**
     * ✅ FETCH & CACHE (Hàm fetch thực sự / oschestrator các provider thành một mảng List
     */
    private Mono<List<Provider>> fetchAndCacheData(String animeId, long cacheTimeSeconds
    ) {
        log.debug("🔍 Starting provider fetch for anime ID: {}", animeId);

        return malSyncClient.fetchMalSync(animeId)
                .flatMap(entries -> {
                    List<Mono<Provider>> tasks = new ArrayList<>();

                    if (entries.length == 0) {
                        log.info("⚠️ No MalSync data, using fallback providers");

                        // Fallback: Consumet + Anify + AnimePahe
                        tasks.add(consumetClient.fetchConsumet(animeId));

                        Mono<List<Provider>> anifyMono = anifyClient.fetchAnify(animeId);
                        tasks.add(anifyMono.flatMapMany(Flux::fromIterable)
                                .collectList()
                                .map(list -> list.isEmpty() ?
                                        new Provider("anify", "anify", new ArrayList<>()) : list.get(0)));

                        tasks.add(animePaheClient.fetchAnimePahe(animeId));

                        return Mono.zip(tasks, objects ->
                                Arrays.stream(objects)
                                        .map(obj -> (Provider) obj)
                                        .collect(Collectors.toList())
                        );
                    }

                    log.debug("✅ MalSync returned {} entries", entries.length);

                    // Build tasks từ MalSync entries
                    for (MalSyncEntry entry : entries) {
                        String providerId = entry.getProviderId();
                        String subUrl = entry.getSub();

                        if ("zoro".equals(providerId) && subUrl != null) {
                            String zoroId = extractIdFromUrl("zoro", subUrl);
                            if (zoroId != null) {
                                tasks.add(zoroClient.fetchZoro(zoroId));
                                tasks.add(nineAnimeClient.fetch9anime(zoroId));  // ← THÊM
                            }
                        } else if ("gogoanime".equals(providerId)) {
                            String gogoId = subUrl != null ? extractIdFromUrl("gogoanime", subUrl) : null;
                            if (gogoId != null) {
                                tasks.add(gogoanimeClient.fetchGogoanime(gogoId));
                            }
                        }
                    }

                    // ✅ LUÔN LUÔN thêm AnimePahe
                    log.debug("📌 Adding AnimePahe provider");
                    tasks.add(animePaheClient.fetchAnimePahe(animeId));

                    if (tasks.isEmpty()) {
                        return Mono.just(Collections.<Provider>emptyList());
                    }

                    return Mono.zip(tasks, objects ->
                            Arrays.stream(objects)
                                    .map(obj -> (Provider) obj)
                                    .collect(Collectors.toList())
                    );
                })
                .flatMap(providers -> {
                    // Filter valid providers
                    List<Provider> validProviders = providers.stream()
                            .filter(Objects::nonNull)
                            .filter(this::hasEpisodes)
                            .collect(Collectors.toList());

                    log.info("✅ Fetched {} valid providers", validProviders.size());

                    if (validProviders.isEmpty()) {
                        log.warn("⚠️ No episodes found for anime ID: {}", animeId);
                        return Mono.just(Collections.<Provider>emptyList());
                    }

                    // Fetch and merge metadata//////////////////////////////////////////////////////
                    return aniZipClient.fetchEpisodeMeta(animeId)
                            .flatMap(metaList -> {
                                log.debug("✅ Fetched {} metadata entries", metaList.size());
                                combineEpisodeMeta(validProviders, metaList);

                                // Cache both
                                return Mono.zip(
                                        redisRepository.setCachedData("episode:" + animeId, validProviders, cacheTimeSeconds),
                                        redisRepository.setCachedData("meta:" + animeId, metaList, cacheTimeSeconds)
                                ).thenReturn(validProviders);
                            })
                            .defaultIfEmpty(validProviders);
                })
                .doOnSuccess(providers -> {
                    if (providers != null) {
                        log.info("✅ Successfully fetched {} providers for anime ID: {}",
                                providers.size(), animeId);
                    }
                });
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private boolean hasEpisodes(Provider provider) {
        Object eps = provider.getEpisodes();
        if (eps instanceof List) {
            return !((List<?>) eps).isEmpty();
        }
        if (eps instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) eps;
            return map.values().stream()
                    .anyMatch(v -> v instanceof List && !((List<?>) v).isEmpty());
        }
        return false;
    }

    private String extractIdFromUrl(String providerName, String url) {
        if (url == null || url.isEmpty()) return null;

        return switch (providerName) {
            case "zoro" -> {
                String[] parts = url.split("/");
                yield parts.length > 0 ? parts[parts.length - 1] : null;
            }
            case "gogoanime" -> url.replace("https://anitaku.to/category/", "")
                    .replace("https://gogoanime.tv/category/", "");
            default -> null;
        };
    }

    private Mono<List<Provider>> getCachedProviders(String key) {
        return redisRepository.getCachedData(key)
                .flatMap(json -> {
                    try {
                        List<Provider> providers = objectMapper.readValue(
                                json, new TypeReference<List<Provider>>() {}
                        );
                        return Mono.just(providers);
                    } catch (Exception e) {
                        log.error("❌ Cache parse error for key: {}", key, e);
                        return redisRepository.deleteKey(key).then(Mono.empty());
                    }
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    private Mono<List<EpisodeMeta>> getCachedMeta(String key) {
        return redisRepository.getCachedData(key)
                .flatMap(json -> {
                    try {
                        List<EpisodeMeta> meta = objectMapper.readValue(
                                json, new TypeReference<List<EpisodeMeta>>() {}
                        );
                        return Mono.just(meta);
                    } catch (Exception e) {
                        log.error("❌ Cache parse error for key: {}", key, e);
                        return redisRepository.deleteKey(key).then(Mono.empty());
                    }
                })
                .defaultIfEmpty(Collections.emptyList());
    }

    private void combineEpisodeMeta(List<Provider> providers, List<EpisodeMeta> metaList) {
        Map<String, EpisodeMeta> metaMap = metaList.stream()
                .collect(Collectors.toMap(EpisodeMeta::getEpisode, m -> m, (a, b) -> a));

        log.debug("🔗 Merging metadata into {} providers", providers.size());

        for (Provider provider : providers) {
            Object episodesObj = provider.getEpisodes();
            if (episodesObj == null) continue;

            if (episodesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Episode> episodes = (List<Episode>) episodesObj;
                episodes.forEach(ep -> episodeMapper.mergeMetadata(ep, metaMap));
            } else if (episodesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, List<Episode>> episodesMap = (Map<String, List<Episode>>) episodesObj;
                episodesMap.values().forEach(episodeList ->
                        episodeList.forEach(ep -> episodeMapper.mergeMetadata(ep, metaMap))
                );
            }
        }
    }
}