package com.animeflix.animestream.service;

import com.animeflix.animestream.exception.StreamFetchException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client to fetch data from AniZip API.
 */
@Component
public class AniZipClient {

    private static final Logger log = LoggerFactory.getLogger(AniZipClient.class);
    private final WebClient webClient;

    public AniZipClient(WebClient animappingWebClient) {
        this.webClient = animappingWebClient;
    }

    /**
     * Fetch MAL ID from AniZip using Anilist ID.
     * @param anilistId Anilist ID (e.g., "21" for One Piece).
     * @return Mono emitting MAL ID as String, or null if not found.
     */
    public Mono<String> fetchMalIdFromAnilist(String anilistId) {
        String uri = "/mappings?anilist_id=" + anilistId;
        log.info("Fetching AniZip MAL ID for Anilist ID: {} with URI: {}", anilistId, uri);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(json -> log.info("Fetched AniZip response for ID {}: {}", anilistId, json))
                .map(response -> response.path("mappings").path("mal_id").asText(null))
                .defaultIfEmpty(null)
                .onErrorResume(e -> {
                    log.error("Error fetching AniZip MAL ID for Anilist ID: {}", anilistId, e);
                    return Mono.just(null); // Match route.tsx behavior
                });
    }
}