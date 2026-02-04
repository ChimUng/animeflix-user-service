package com.animeflix.animeepisode.util;

import com.animeflix.animeepisode.service.stream.AniZipClient;
import com.animeflix.animeepisode.service.stream.MalSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 🔧 SlugBuilder - Utility class để build Zoro/9anime episode IDs
 *
 * Reusable logic cho ZoroStreamClient và NineAnimeStreamClient
 *
 * Format output: "{slug}?ep={episodeId}"
 * Example: "steinsgate-0-92?ep=3303"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlugBuilder {

    private final MalSyncClient malSyncClient;
    private final AniZipClient aniZipClient;

    /**
     * Build Zoro/9anime episode ID
     *
     * Logic (1:1 với Next.js buildZoroAnimeEpisodeId):
     * 1. Kiểm tra nếu episodeId đã có "?ep=" → return ngay
     * 2. Try MalSync với anilistId → lấy slug
     * 3. Nếu không có slug:
     *    - Call AniZip → lấy malId
     *    - Call MalSync với malId → lấy slug
     * 4. Return "{slug}?ep={episodeId}" hoặc episodeId gốc nếu fail
     *
     * @param anilistId  AniList ID của anime
     * @param episodeId  Episode ID (có thể là số thuần hoặc đã build)
     * @return           Built episode ID hoặc episodeId gốc
     */
    public Mono<String> buildZoroEpisodeId(String anilistId, String episodeId) {
        // ✅ Check 1: Nếu đã có "?ep=" → đã build rồi
        if (episodeId.contains("?ep=")) {
            log.info("✅ SlugBuilder: episodeId đã ở dạng đầy đủ: {}", episodeId);
            return Mono.just(episodeId);
        }

        log.info("🔨 SlugBuilder: Building episodeId từ anilistId={}, episodeId={}",
                anilistId, episodeId);

        // ✅ Try 1: MalSync với AniList ID
        return malSyncClient.getZoroSlug(anilistId)
                .flatMap(map -> {
                    String slug = (String) map.get("slug");

                    if (slug != null && !slug.isEmpty()) {
                        String result = slug + "?ep=" + episodeId;
                        log.info("✅ SlugBuilder: Built from MalSync(anilist): {}", result);
                        return Mono.just(result);
                    }

                    // ✅ Try 2: AniZip → MAL ID → MalSync
                    log.debug("🔄 SlugBuilder: MalSync(anilist) failed, trying AniZip...");
                    return aniZipClient.fetchMalIdFromAnilist(anilistId)
                            .flatMap(malId -> {
                                if (malId == null || malId.equals(anilistId)) {
                                    log.warn("⚠️ SlugBuilder: No MAL ID fallback");
                                    return Mono.just(episodeId);
                                }

                                log.debug("🔄 SlugBuilder: Trying MalSync with MAL ID: {}", malId);
                                return malSyncClient.getZoroSlug(malId)
                                        .map(map2 -> {
                                            String slug2 = (String) map2.get("slug");
                                            if (slug2 != null && !slug2.isEmpty()) {
                                                String result = slug2 + "?ep=" + episodeId;
                                                log.info("✅ SlugBuilder: Built from MalSync(mal): {}", result);
                                                return result;
                                            }
                                            log.warn("⚠️ SlugBuilder: MalSync(mal) failed");
                                            return episodeId;
                                        });
                            })
                            .defaultIfEmpty(episodeId);
                })
                .defaultIfEmpty(episodeId)
                .doOnNext(result -> {
                    if (result.equals(episodeId) && !result.contains("?ep=")) {
                        log.warn("⚠️ SlugBuilder: Fallback to original episodeId: {}", episodeId);
                    }
                });
    }

    /**
     * Validate nếu episodeId đã được build chưa
     *
     * @param episodeId  Episode ID cần check
     * @return           true nếu đã build (có "?ep=")
     */
    public boolean isBuilt(String episodeId) {
        return episodeId != null && episodeId.contains("?ep=");
    }

    /**
     * Extract slug từ built episodeId
     *
     * Example: "steinsgate-0-92?ep=3303" → "steinsgate-0-92"
     *
     * @param builtEpisodeId  Built episode ID
     * @return                 Slug hoặc null nếu invalid
     */
    public String extractSlug(String builtEpisodeId) {
        if (builtEpisodeId == null || !builtEpisodeId.contains("?ep=")) {
            return null;
        }
        return builtEpisodeId.split("\\?ep=")[0];
    }

    /**
     * Extract episode number từ built episodeId
     *
     * Example: "steinsgate-0-92?ep=3303" → "3303"
     *
     * @param builtEpisodeId  Built episode ID
     * @return                 Episode number hoặc null nếu invalid
     */
    public String extractEpisodeNumber(String builtEpisodeId) {
        if (builtEpisodeId == null || !builtEpisodeId.contains("?ep=")) {
            return null;
        }
        String[] parts = builtEpisodeId.split("\\?ep=");
        return parts.length > 1 ? parts[1] : null;
    }
}