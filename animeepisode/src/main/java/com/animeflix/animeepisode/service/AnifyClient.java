package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AnifyClient {

    private final WebClient webClient;

    public AnifyClient(WebClient anifyWebClient) {
        this.webClient = anifyWebClient;
    }

    public Mono<List<Provider>> fetchAnify(String id) {
        String uri = "/info/" + id + "?fields=[episodes]";
        return webClient.get().uri(uri).retrieve()
                .bodyToMono(AnifyProvider[].class)
                .defaultIfEmpty(new AnifyProvider[0])
                .flatMapMany(Flux::fromArray)
                .filter(ep -> !"9anime".equals(ep.getProviderId()))
                .map(ep -> {
                    String providerId = "gogoanime".equals(ep.getProviderId()) ? "gogobackup" : ep.getProviderId();
                    List<Episode> episodes = ep.getEpisodes().stream()
                                    .map(this::toEpisode)
                                    .collect(Collectors.toList());
                    return new Provider(providerId, providerId, false, episodes);
                })
                .collectList()
                .onErrorMap(e -> new EpisodeFetchException("Anify fetch failed for ID: " + id, e));
    }

    private Episode toEpisode(AnifyEpisode anifyEp) {
        Episode ep = new Episode();
        ep.setEpisodeId(anifyEp.getEpisodeId());
        ep.setNumber(anifyEp.getNumber());
        ep.setTitle(anifyEp.getTitle());
        ep.setImage(anifyEp.getImage());
        ep.setDescription(anifyEp.getDescription());
        ep.setIsFiller(anifyEp.getIsFiller());
        return ep;
    }
}