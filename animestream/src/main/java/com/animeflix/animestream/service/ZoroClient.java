package com.animeflix.animestream.service;

import com.animeflix.animestream.exception.StreamFetchException;
import com.animeflix.animestream.Model.StreamResponse;
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

@Component
public class ZoroClient {

    private final WebClient webClient;
    private final MalSyncClient malSyncClient;

    public ZoroClient(WebClient zoroWebClient, MalSyncClient malSyncClient) {
        this.webClient = zoroWebClient;
        this.malSyncClient = malSyncClient;
    }

    public Mono<StreamResponse> fetchZoroEpisode(String provider, String episodeId, int episodeNum, String id, String subtype) {
        String uri = "/anime/episode-srcs?id=" + episodeId + "&server_id=&category=" + subtype;
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> malSyncClient.getZoroSlug(id)
                        .map(ids -> parseStreamResponse(response, ids))
                        .defaultIfEmpty(parseStreamResponse(response, new HashMap<>())))
                .onErrorMap(e -> new StreamFetchException("Zoro fetch failed for episode ID: " + episodeId, e));
    }

    private StreamResponse parseStreamResponse(JsonNode response, Map<String, Object> ids) {
        StreamResponse streamResponse = new StreamResponse();
        streamResponse.setSuccess(response.path("success").asBoolean(true)); // Kiểm tra success từ API nếu có

        StreamResponse.Data data = new StreamResponse.Data();
        data.setHeaders(parseHeaders(response.path("headers")));
        data.setSources(parseSources(response.path("sources")));
        data.setSubtitles(parseSubtitles(response.path("subtitles")));

        // Ưu tiên lấy anilistID và malID từ MalSyncClient, nếu không thì từ response
        data.setAnilistID((Integer) ids.getOrDefault("anilistID", response.path("anilistID").asInt(0) == 0 ? null : response.path("anilistID").asInt()));
        data.setMalID((Integer) ids.getOrDefault("malID", response.path("malID").asInt(0) == 0 ? null : response.path("malID").asInt()));

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
                subtitle.setLang(subtitleNode.path("language").asText(""));
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