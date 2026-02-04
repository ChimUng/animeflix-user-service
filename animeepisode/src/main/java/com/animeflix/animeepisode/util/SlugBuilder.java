package com.animeflix.animeepisode.util;

import com.animeflix.animeepisode.service.AniZipClient;
import com.animeflix.animeepisode.service.MalSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlugBuilder {

    private final MalSyncClient malSyncClient;
    private final AniZipClient aniZipClient;

    /**
     * Build Zoro/9anime episode ID
     *
     * @param anilistId  AniList ID
     * @param episodeId  Episode ID (number or built)
     * @return           "{slug}?ep={episodeId}" hoặc episodeId gốc
     */
    public Mono<String> buildZoroEpisodeId(String anilistId, String episodeId) {
        // 1. Already built?
        if (episodeId.contains("?ep=")) {
            log.info("✅ SlugBuilder: episodeId đã built: {}", episodeId);
            return Mono.just(episodeId);
        }

        log.info("🔨 SlugBuilder: Building episodeId từ anilistId={}, episodeId={}", anilistId, episodeId);

        // 2. Try MalSync (WebClient có timeout config rồi, không cần .timeout() ở đây)
        return malSyncClient.getZoroSlug(anilistId)
                .flatMap(slug -> {
                    if (slug != null && !slug.isEmpty()) {
                        String result = slug + "?ep=" + episodeId;
                        log.info("✅ SlugBuilder: Built from MalSync(anilist): {}", result);
                        return Mono.just(result);
                    }

                    // 3. Fallback: AniZip → MAL ID → MalSync
                    log.debug("🔄 SlugBuilder: MalSync(anilist) empty, trying AniZip...");
                    return tryAniZipFallback(anilistId, episodeId);
                })
                .onErrorResume(e -> {
                    log.warn("⚠️ SlugBuilder: MalSync error ({}), trying AniZip fallback", e.getMessage());
                    return tryAniZipFallback(anilistId, episodeId);
                })
                .defaultIfEmpty(episodeId)
                .doOnNext(result -> {
                    if (result.equals(episodeId) && !result.contains("?ep=")) {
                        log.warn("⚠️ SlugBuilder: All methods failed, using original episodeId: {}", episodeId);
                    }
                });
    }

    /**
     * AniZip fallback chain
     */
    private Mono<String> tryAniZipFallback(String anilistId, String episodeId) {
        return aniZipClient.fetchMalIdFromAnilist(anilistId)
                .flatMap(malId -> {
                    if (malId == null || malId.isEmpty() || malId.equals(anilistId)) {
                        log.warn("⚠️ SlugBuilder: No valid MAL ID from AniZip");
                        return Mono.just(episodeId);
                    }

                    log.debug("🔄 SlugBuilder: Trying MalSync with MAL ID: {}", malId);
                    return malSyncClient.getZoroSlug(malId)
                            .map(slug -> {
                                if (slug != null && !slug.isEmpty()) {
                                    String result = slug + "?ep=" + episodeId;
                                    log.info("✅ SlugBuilder: Built from MalSync(mal): {}", result);
                                    return result;
                                }
                                log.warn("⚠️ SlugBuilder: MalSync(mal) returned empty");
                                return episodeId;
                            })
                            .onErrorResume(e -> {
                                log.warn("⚠️ SlugBuilder: MalSync(mal) error: {}", e.getMessage());
                                return Mono.just(episodeId);
                            });
                })
                .defaultIfEmpty(episodeId)
                .onErrorResume(e -> {
                    log.warn("⚠️ SlugBuilder: AniZip fallback error: {}", e.getMessage());
                    return Mono.just(episodeId);
                });
    }

    /**
     * Check if episodeId is already built
     */
    public boolean isBuilt(String episodeId) {
        return episodeId != null && episodeId.contains("?ep=");
    }

    /**
     * Extract slug: "steinsgate-0-92?ep=3303" → "steinsgate-0-92"
     */
    public String extractSlug(String builtEpisodeId) {
        if (builtEpisodeId == null || !builtEpisodeId.contains("?ep=")) {
            return null;
        }
        return builtEpisodeId.split("\\?ep=")[0];
    }

    /**
     * Extract episode number: "steinsgate-0-92?ep=3303" → "3303"
     */
    public String extractEpisodeNumber(String builtEpisodeId) {
        if (builtEpisodeId == null || !builtEpisodeId.contains("?ep=")) {
            return null;
        }
        String[] parts = builtEpisodeId.split("\\?ep=");
        return parts.length > 1 ? parts[1] : null;
    }
}