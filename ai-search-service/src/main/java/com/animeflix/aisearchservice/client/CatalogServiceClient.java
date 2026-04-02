package com.animeflix.aisearchservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Gọi anime-catalog-service để lấy data anime cho batch embedding.
 * Dùng REST API của catalog service thay vì direct DB access.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogServiceClient {

    private final WebClient catalogWebClient;

    /**
     * Lấy tất cả anime từ catalog service theo từng page.
     * Trả về Flux stream để xử lý từng page một.
     */
    public Flux<JsonNode> fetchAllAnime(int perPage) {
        return fetchPage(1, perPage)
                .expand(response -> {
                    boolean hasNextPage = response
                            .path("data")
                            .path("pageInfo")
                            .path("hasNextPage")
                            .asBoolean(false);

                    if (!hasNextPage) {
                        return Mono.empty();
                    }

                    int nextPage = response
                            .path("data")
                            .path("pageInfo")
                            .path("currentPage")
                            .asInt(1) + 1;

                    log.info("📄 Fetching catalog page: {}", nextPage);
                    return fetchPage(nextPage, perPage);
                })
                .flatMap(response -> {
                    JsonNode dataNode = response.path("data");
                    List<JsonNode> animes = new ArrayList<>();
                    if (dataNode.isArray()) {
                        dataNode.elements().forEachRemaining(animes::add);
                    }
                    return Flux.fromIterable(animes);
                });
    }

    private Mono<JsonNode> fetchPage(int page, int perPage) {
        return catalogWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/popular")
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("❌ Error fetching catalog page {}: {}", page, e.getMessage()));
    }
}