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

/**
 * 9anime Client - Fetch từ Zenime API
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
     * @param zoroId Zoro ID (e.g., "one-piece-100")
     * @return Provider với episodes
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
                        ep.setId(epNode.path("id").asText());

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

                    return new Provider("9anime", "9anime", episodes);
                })
                .onErrorResume(e -> {
                    log.error("❌ Error fetching 9anime for {}: {}", zoroId, e.getMessage());
                    return Mono.just(emptyProvider());
                });
    }

    private Provider emptyProvider() {
        return new Provider("9anime", "9anime", new ArrayList<>());
    }
}