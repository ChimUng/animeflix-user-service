package com.animeflix.animestream.service;

import com.animeflix.animestream.exception.StreamFetchException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Client to fetch data from MalSync API.
 */
@Component
public class MalSyncClient {

    private final WebClient webClient;

    public MalSyncClient(WebClient malsyncWebClient) {
        this.webClient = malsyncWebClient;
    }

    public Mono<Map<String, Object>> getZoroSlug(String id) {
        return webClient.get()
                .uri("/" + id)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jsonNode -> {
                    Map<String, Object> result = new HashMap<>();
                    JsonNode sites = jsonNode.path("Sites");
                    if (sites.isMissingNode()) return result;

                    // Duyệt qua các provider trong Sites
                    sites.fields().forEachRemaining(entry -> {
                        if ("zoro".equalsIgnoreCase(entry.getKey())) {
                            JsonNode zoroSite = entry.getValue();
                            if (zoroSite.elements().hasNext()) {
                                JsonNode firstEntry = zoroSite.elements().next();
                                String url = firstEntry.path("url").asText("");
                                if (!url.isEmpty()) {
                                    String slug = url.substring(url.lastIndexOf('/') + 1);
                                    result.put("slug", slug);
                                    result.put("anilistID", firstEntry.path("aniId").asInt(0) == 0 ? null : firstEntry.path("aniId").asInt());
                                    result.put("malID", firstEntry.path("malId").asInt(0) == 0 ? null : firstEntry.path("malId").asInt());
                                }
                            }
                        }
                    });
                    return result;
                })
                .defaultIfEmpty(new HashMap<>())
                .onErrorMap(e -> new StreamFetchException("MalSync fetch failed for ID: " + id, e));
    }
}
