package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.MalSyncEntry;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class MalSyncClient {

    private final WebClient webClient;

    public MalSyncClient(WebClient malsyncWebClient) {
        this.webClient = malsyncWebClient;
    }

    public Mono<MalSyncEntry[]> fetchMalSync(String id) {
        return webClient.get().uri("/" + id).retrieve()
                .bodyToMono(Map.class)
                .map(data -> {
                    if (data == null || !data.containsKey("Sites")) return new MalSyncEntry[0];
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sites = (Map<String, Object>) data.get("Sites");

                    return sites.entrySet().stream()
                            .filter(entry -> List.of("zoro", "gogoanime", "kickassanime", "netflix").contains(entry.getKey().toLowerCase()))
                            .map(entry -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> siteData = (Map<String, Object>) entry.getValue();

                                // Lấy entry đầu tiên trong từng site
                                String firstUrl = siteData.values().stream()
                                        .filter(Map.class::isInstance)
                                        .map(o -> (Map<String, Object>) o)
                                        .map(o -> (String) o.get("url"))
                                        .findFirst()
                                        .orElse("");

                                return new MalSyncEntry(entry.getKey().toLowerCase(), firstUrl, null);
                            })
                            .toArray(MalSyncEntry[]::new);
                })
                .defaultIfEmpty(new MalSyncEntry[0])
                .onErrorMap(e -> new EpisodeFetchException("MalSync fetch failed for ID: " + id, e));
    }

}