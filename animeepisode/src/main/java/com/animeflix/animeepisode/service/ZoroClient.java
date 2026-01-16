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

    public Mono<Provider> fetchZoro(String id) {
        String uri = "/anime/" + id + "/episodes";
        return zoroWebClient.get().uri(uri).retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(response -> log.info("Zoro response for ID {}: {}", id, response))
                .map(response -> {
                    List<Episode> episodes = new ArrayList<>();
                    JsonNode episodesNode = response.path("data").path("episodes");
                    if (episodesNode.isArray()) {  // Giả định response là array episodes
                        for (JsonNode epNode : episodesNode) {
                            Episode ep = new Episode();
                            ep.setEpisodeId(epNode.path("episodeId").asText(""));
                            ep.setNumber(epNode.path("number").asInt(0));
                            ep.setTitle(epNode.path("title").asText(""));
                            ep.setImage(epNode.path("image").asText(""));
                            ep.setDescription(epNode.path("description").asText(""));
                            ep.setIsFiller(epNode.path("isFiller").asBoolean(false));
                            episodes.add(ep);
                        }
                    }
                    // Assume sub only, no dub
                    return new Provider("zoro", "zoro", false, Map.of("sub", episodes));
                })
                .onErrorResume(e -> {
                    log.error("Error fetching Zoro episodes for ID: {}", id, e);
                    return Mono.empty();
                });  // Hoặc throw
    }
}