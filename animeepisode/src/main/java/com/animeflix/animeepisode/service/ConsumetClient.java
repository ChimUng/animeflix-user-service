package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.Episode;
import com.animeflix.animeepisode.model.Provider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ConsumetClient {

    private final WebClient webClient;

    public ConsumetClient(WebClient consumetWebClient) {
        this.webClient = consumetWebClient;
    }

    public Mono<Provider> fetchConsumet(String id) {  // Return single Provider
        return fetchData(id, false).zipWith(fetchData(id, true))
                .map(tuple -> {
                    List<Episode> subData = tuple.getT1();
                    List<Episode> dubData = tuple.getT2();
                    Map<String, List<Episode>> episodes = Map.of(
                            "sub", subData,
                            "dub", dubData
                    );
                    return new Provider("gogoanime", "gogoanime", true, episodes);
                })
                .onErrorMap(e -> new EpisodeFetchException("Consumet fetch failed for ID: " + id, e));
    }

    private Mono<List<Episode>> fetchData(String id, boolean dub) {
        String uri = "/meta/anilist/episodes/" + id + (dub ? "?dub=true" : "");
        return webClient.get().uri(uri).retrieve()
                .bodyToFlux(Episode.class)
                .collectList()
                .defaultIfEmpty(List.of())
                .onErrorMap(e -> new EpisodeFetchException("Consumet " + (dub ? "dub" : "sub") + " failed: " + id, e));
    }
}