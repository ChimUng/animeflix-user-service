package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.EpisodeMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class AniZipClient {

    private static final Logger log = LoggerFactory.getLogger(AniZipClient.class);
    private final WebClient animappingWebClient;

    public AniZipClient(WebClient animappingWebClient) {
        this.animappingWebClient = animappingWebClient;
    }

    /**
     * ✅ Method gốc - Fetch episode metadata
     */
    public Mono<List<EpisodeMeta>> fetchEpisodeMeta(String anilistId) {
        return getAniZipResponse(anilistId)
                .map(this::extractEpisodesFromResponse)
                .onErrorMap(e -> new EpisodeFetchException("AniZip fetch failed for ID: " + anilistId, e));
    }

    /**
     * 🆕 NEW METHOD - Lấy MAL ID từ AniList ID
     *
     * Used by: ZoroStreamClient, NineAnimeStreamClient (fallback khi MalSync fail)
     *
     * Flow:
     * 1. Call AniZip API: GET https://api.ani.zip/mappings?anilist_id={id}
     * 2. Parse response: { "mappings": { "mal_id": 12345, ... } }
     * 3. Return mal_id as String
     *
     * @param anilistId AniList ID
     * @return MAL ID (as String) hoặc null nếu không tìm thấy
     */
    public Mono<String> fetchMalIdFromAnilist(String anilistId) {
        log.debug("🔍 AniZip: Fetching MAL ID for AniList ID: {}", anilistId);

        String uri = "/mappings?anilist_id=" + anilistId;

        return animappingWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode mappingsNode = response.path("mappings");

                    if (mappingsNode.isMissingNode() || mappingsNode.isNull()) {
                        log.warn("⚠️ AniZip: No mappings for AniList ID: {}", anilistId);
                        return null;
                    }

                    JsonNode malIdNode = mappingsNode.path("mal_id");

                    if (malIdNode.isMissingNode() || malIdNode.isNull()) {
                        log.warn("⚠️ AniZip: No MAL ID in mappings for AniList ID: {}", anilistId);
                        return null;
                    }

                    // MAL ID có thể là number hoặc string
                    String malId = malIdNode.isNumber()
                            ? String.valueOf(malIdNode.asLong())
                            : malIdNode.asText();

                    if (malId.isEmpty()) {
                        log.warn("⚠️ AniZip: Empty MAL ID for AniList ID: {}", anilistId);
                        return null;
                    }

                    log.info("✅ AniZip: Found MAL ID: {} for AniList ID: {}", malId, anilistId);
                    return malId;
                })
                .onErrorResume(e -> {
                    log.error("❌ AniZip: Error fetching MAL ID for AniList ID {}: {}",
                            anilistId, e.getMessage());
                    return Mono.just(null);
                });
    }

    /**
     * ✅ Private method - Gọi API AniZip (reused)
     */
    private Mono<JsonNode> getAniZipResponse(String anilistId) {
        String uri = "/mappings?anilist_id=" + anilistId;
        log.debug("🔍 AniZip fetching: {}", uri);

        return animappingWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(json -> log.debug("Fetched AniZip response for {}: {}", anilistId, json))
                .onErrorResume(e -> {
                    log.error("Error fetching AniZip data for {}", anilistId, e);
                    return Mono.empty();
                });
    }

    /**
     * ✅ Private method - Extract episodes (unchanged)
     */
    private List<EpisodeMeta> extractEpisodesFromResponse(JsonNode root) {
        if (root == null) {
            log.warn("⚠️ AniZip: root is null");
            return List.of();
        }

        JsonNode episodesNode = root.path("data").path("episodes");

        if (episodesNode.isMissingNode() || episodesNode.isNull()) {
            episodesNode = root.path("episodes");
        }

        if (episodesNode.isMissingNode() || episodesNode.isNull() || !episodesNode.isObject()) {
            log.warn("⚠️ AniZip: No episodes found in response. Keys: {}", root.fieldNames());
            return List.of();
        }

        List<EpisodeMeta> episodes = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = episodesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode episodeNode = entry.getValue();

            EpisodeMeta episodeMeta = buildEpisodeMeta(episodeNode);
            episodes.add(episodeMeta);
        }

        log.info("✅ AniZip: Extracted {} episode metadata entries", episodes.size());

        episodes.sort(Comparator.comparing(EpisodeMeta::getEpisode));
        return episodes;
    }

    /**
     * ✅ Private method - Build EpisodeMeta (unchanged)
     */
    private EpisodeMeta buildEpisodeMeta(JsonNode node) {
        String episodeNumber = node.path("episode").asText("");
        String summary = node.path("summary").asText("");
        String image = node.path("image").asText("");

        Map<String, String> titleMap = new HashMap<>();
        JsonNode titleNode = node.path("title");
        if (titleNode.isObject()) {
            titleNode.fieldNames().forEachRemaining(key -> {
                titleMap.put(key, titleNode.path(key).asText(""));
            });
        } else if (titleNode.isTextual()) {
            titleMap.put("default", titleNode.asText());
        }

        Map<String, String> filteredTitle = new LinkedHashMap<>();
        if (titleMap.containsKey("x-jat")) filteredTitle.put("x-jat", titleMap.get("x-jat"));
        if (titleMap.containsKey("en")) filteredTitle.put("en", titleMap.get("en"));
        if (filteredTitle.isEmpty() && !titleMap.isEmpty()) {
            Map.Entry<String, String> first = titleMap.entrySet().iterator().next();
            filteredTitle.put(first.getKey(), first.getValue());
        }

        EpisodeMeta meta = new EpisodeMeta();
        meta.setEpisode(episodeNumber);
        meta.setSummary(summary);
        meta.setImage(image);
        meta.setTitle(filteredTitle);

        return meta;
    }
}