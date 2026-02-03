package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 9anime Client - Fetch từ Zenime API
 *
 * ✅ Output structure: Map<String, List<Episode>> {"sub": [...]}
 * -> Consistent với Zoro, Gogoanime providers
 */
@Component
@Slf4j
public class NineAnimeClient {

    private final WebClient webClient;

    public NineAnimeClient(@Value("${zenime.url:https://zenime-api.vercel.app}") String zenimeUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(zenimeUrl)
                .build();
    }

    /**
     * Fetch 9anime episodes từ Zenime API
     *
     * Zenime response structure:
     * {
     *   "success": true,
     *   "results": {
     *     "totalEpisodes": 1085,
     *     "episodes": [
     *       {
     *         "episode_no": 1,
     *         "id": "one-piece-100?ep=2142",
     *         "title": "I'm Luffy!...",
     *         "japanese_title": "...",
     *         "filler": false
     *       }
     *     ]
     *   }
     * }
     *
     * @param zoroId Zoro ID (e.g., "one-piece-100")
     * @return Provider với episodes wrapped trong {"sub": [...]}
     */
    public Mono<Provider> fetch9anime(String zoroId) {
        if (zoroId == null || zoroId.isEmpty()) {
            log.debug("⚠️ 9anime: No Zoro ID provided");
            return Mono.just(emptyProvider());
        }

        String uri = "/api/episodes/" + zoroId;
        log.debug("🔍 Fetching 9anime: {}", uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    // Check success
                    if (!response.path("success").asBoolean(false)) {
                        log.warn("⚠️ 9anime API returned success=false for ID: {}", zoroId);
                        return emptyProvider();
                    }

                    JsonNode resultsNode = response.path("results");
                    JsonNode episodesNode = resultsNode.path("episodes");

                    if (!episodesNode.isArray() || episodesNode.isEmpty()) {
                        log.warn("⚠️ No episodes from 9anime for ID: {}", zoroId);
                        return emptyProvider();
                    }

                    // Parse episodes
                    List<Episode> episodes = new ArrayList<>();
                    episodesNode.forEach(epNode -> {
                        Episode ep = new Episode();
                        ep.setNumber(epNode.path("episode_no").asInt());

                        // ✅ "id" từ Zenime giống format episodeId của Zoro (e.g. "one-piece-100?ep=2142")
                        ep.setEpisodeId(epNode.path("id").asText());

                        // Title: ưu tiên title > japanese_title
                        String title = epNode.path("title").asText();
                        if (title.isEmpty()) {
                            title = epNode.path("japanese_title").asText();
                        }
                        ep.setTitle(title.isEmpty() ? null : title);

                        ep.setIsFiller(epNode.path("filler").asBoolean(false));

                        episodes.add(ep);
                    });

                    log.info("✅ 9anime: Found {} episodes for {}", episodes.size(), zoroId);

                    // ✅ FIX: Wrap trong Map {"sub": episodes} để consistent với Zoro/Gogoanime
                    return new Provider("9anime", "9anime", false, Map.of("sub", episodes));
                })
                .onErrorResume(e -> {
                    log.error("❌ Error fetching 9anime for {}: {}", zoroId, e.getMessage());
                    return Mono.just(emptyProvider());
                });
    }

    private Provider emptyProvider() {
        // ✅ Empty provider cũng phải consistent structure
        return new Provider("9anime", "9anime", false, Map.of("sub", new ArrayList<>()));
    }
}