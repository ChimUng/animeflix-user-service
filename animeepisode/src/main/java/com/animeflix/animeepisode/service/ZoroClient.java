package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ZoroClient {

    private static final Logger log = LoggerFactory.getLogger(ZoroClient.class);
    private final WebClient zoroWebClient;

    public ZoroClient(WebClient zoroWebClient) {
        this.zoroWebClient = zoroWebClient;
    }

    /**
     * Fetch episodes từ Zoro API
     *
     * ✅ Zoro API ACTUAL response structure (verified):
     * {
     *   "status": 200,
     *   "data": {
     *     "totalEpisodes": 25,
     *     "episodes": [
     *       {
     *         "title": "To You Two Thousand Years Later",
     *         "episodeId": "attack-on-titan-112?ep=3303",  ← ĐÚNG field name
     *         "number": 1,
     *         "isFiller": false
     *       }
     *     ]
     *   }
     * }
     *
     * Structure output: Map<String, List<Episode>> {"sub": [...]}
     * -> Consistent với Gogoanime provider
     */
    public Mono<Provider> fetchZoro(String id) {
        String uri = "/anime/" + id + "/episodes";
        return zoroWebClient.get().uri(uri).retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(response -> log.debug("Zoro response for ID {}: {}", id, response))
                .map(response -> {
                    List<Episode> episodes = new ArrayList<>();
                    JsonNode episodesNode = response.path("data").path("episodes");

                    if (episodesNode.isArray()) {
                        for (JsonNode epNode : episodesNode) {
                            Episode ep = new Episode();

                            // ✅ FIXED: Zoro API trả "episodeId" field (NOT "id")
                            // Verified từ actual API response
                            ep.setEpisodeId(epNode.path("episodeId").asText(""));

                            ep.setNumber(epNode.path("number").asInt(0));
                            ep.setTitle(epNode.path("title").asText(""));
                            ep.setIsFiller(epNode.path("isFiller").asBoolean(false));

                            // NOTE: Zoro API không trả image/description per episode
                            // Không set "" rỗng -> để null, sẽ được merge từ AniZip sau
                            // JsonInclude.Include.NON_NULL sẽ không serialize null fields

                            episodes.add(ep);
                        }
                    }

                    log.info("✅ Zoro: Found {} episodes for ID: {}", episodes.size(), id);

                    // Structure: {"sub": [episodes]} -> consistent với Gogoanime
                    return new Provider("zoro", "zoro", false, Map.of("sub", episodes));
                })
                .onErrorResume(e -> {
                    log.error("Error fetching Zoro episodes for ID: {}", id, e);
                    return Mono.empty();
                });
    }
}