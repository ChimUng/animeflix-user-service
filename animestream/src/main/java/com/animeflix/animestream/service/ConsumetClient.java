package com.animeflix.animestream.service;

import com.animeflix.animestream.Model.StreamResponse;
import com.animeflix.animestream.exception.StreamFetchException;
import com.animeflix.animestream.Model.RawEpisode;
import com.animeflix.animestream.Model.Source;
import com.animeflix.animestream.Model.Subtitle;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client to fetch stream data from Consumet API.
 */
@Component
public class ConsumetClient {

    private final WebClient webClient;
    private final MalSyncClient malSyncClient;

    public ConsumetClient(WebClient consumetWebClient, MalSyncClient malSyncClient) {
        this.webClient = consumetWebClient;
        this.malSyncClient = malSyncClient;
    }

    /**
     * Fetch stream for a specific episode from Consumet.
     * @param episodeId Episode ID.
     * @param animeId
     * @return Mono emitting StreamResponse.
     */
    public Mono<StreamResponse> fetchConsumetEpisode(String episodeId, String animeId) {
        String uri = "/meta/anilist/watch/" + episodeId;
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> malSyncClient.getZoroSlug(animeId)
                        .map(ids -> parseStreamResponse(response, ids))
                        .defaultIfEmpty(parseStreamResponse(response, new HashMap<>())))
                .onErrorMap(e -> new StreamFetchException("Consumet fetch failed for episode ID: " + episodeId, e));
    }

    private StreamResponse parseStreamResponse(JsonNode response, Map<String, Object> ids) {
        StreamResponse streamResponse = new StreamResponse();
        streamResponse.setSuccess(true); // Giả định thành công, có thể kiểm tra nếu API cung cấp

        StreamResponse.Data data = new StreamResponse.Data();
        data.setHeaders(parseHeaders(response.path("headers")));
        data.setSources(parseSources(response.path("sources")));
        data.setSubtitles(parseSubtitles(response.path("subtitles")));
        data.setAnilistID((Integer) ids.getOrDefault("anilistID", response.path("aniId").asInt(0) == 0 ? null : response.path("aniId").asInt()));
        data.setMalID((Integer) ids.getOrDefault("malID", response.path("malId").asInt(0) == 0 ? null : response.path("malId").asInt()));

        streamResponse.setData(data);
        return streamResponse;
    }

    private List<Source> parseSources(JsonNode sourcesNode) {
        List<Source> sources = new ArrayList<>();
        if (sourcesNode.isArray()) {
            for (JsonNode sourceNode : sourcesNode) {
                Source source = new Source();
                source.setUrl(sourceNode.path("url").asText(""));
                source.setQuality(sourceNode.path("quality").asText(""));
                source.setIsM3U8(sourceNode.path("isM3U8").asBoolean(false));
                sources.add(source);
            }
        }
        return sources;
    }

    private List<Subtitle> parseSubtitles(JsonNode subtitlesNode) {
        List<Subtitle> subtitles = new ArrayList<>();
        if (subtitlesNode.isArray()) {
            for (JsonNode subtitleNode : subtitlesNode) {
                Subtitle subtitle = new Subtitle();
                subtitle.setUrl(subtitleNode.path("url").asText(""));
                subtitle.setLang(subtitleNode.path("lang").asText("")); // API Consumet dùng "lang" thay vì "language"
                subtitles.add(subtitle);
            }
        }
        return subtitles;
    }

    private Map<String, String> parseHeaders(JsonNode headersNode) {
        Map<String, String> headers = new HashMap<>();
        if (headersNode.isObject()) {
            headersNode.fields().forEachRemaining(entry -> {
                headers.put(entry.getKey(), entry.getValue().asText(""));
            });
        }
        return headers;
    }
}