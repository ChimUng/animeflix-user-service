package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GogoanimeClient {

    private final WebClient gogoWebClient;  // From config, base anitaku.to or zoro

    public GogoanimeClient(WebClient gogoWebClient) {
        this.gogoWebClient = gogoWebClient;
    }

    public Mono<Provider> fetchGogoanime(String id) {
        String uri = "/anime/gogoanime/info/" + id;  // Theo docs Consumet
        log.debug("🔍 Fetching Gogoanime: {}", uri);
        return gogoWebClient.get().uri(uri).retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    List<Episode> subEpisodes = parseEpisodes(response.path("episodes"));  // Assume field "episodes" là array sub
                    List<Episode> dubEpisodes = new ArrayList<>();  // Nếu có field "dubEpisodes", parse tương tự; hiện assume no dub or fetch separate
                    boolean hasDub = !dubEpisodes.isEmpty();
                    return new Provider("gogoanime", "gogoanime", hasDub, Map.of("sub", subEpisodes, "dub", dubEpisodes));
                })
                .onErrorResume(e -> Mono.just(new Provider("gogoanime", "gogoanime", false, Map.of())));  // Empty on fail
    }

    private List<Episode> parseEpisodes(JsonNode episodesNode) {
        List<Episode> episodes = new ArrayList<>();
        if (episodesNode.isArray()) {
            for (JsonNode epNode : episodesNode) {
                Episode ep = new Episode();
                ep.setEpisodeId(epNode.path("id").asText());  // Gogo thường dùng "id" như "one-piece-episode-1"
                ep.setNumber(epNode.path("number").asInt());
                ep.setTitle(epNode.path("title").asText());
                ep.setImage(epNode.path("image").asText());  // Nếu có
                ep.setDescription(epNode.path("description").asText());
                ep.setIsFiller(epNode.path("isFiller").asBoolean(false));  // Nếu có
                episodes.add(ep);
            }
        }
        return episodes;
    }
}
