package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.EpisodeMeta;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class EpisodeMetaClient {

    private final WebClient consumetWebClient;

    public EpisodeMetaClient(WebClient consumetWebClient) {
        this.consumetWebClient = consumetWebClient;
    }

    public Mono<List<EpisodeMeta>> fetchEpisodeMeta(String id, boolean useCache) {
        String uri = "/meta/anilist/episodes/" + id;
        return consumetWebClient.get().uri(uri).retrieve()
                .bodyToFlux(EpisodeMeta.class)
                .collectList()
                .defaultIfEmpty(List.of())
                .onErrorMap(e -> new EpisodeFetchException("EpisodeMeta fetch failed for ID: " + id, e));
    }
}
