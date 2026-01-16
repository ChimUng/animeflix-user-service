package com.animeflix.animeepisode.service;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.EpisodeMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class AniZipClient {

    private static final Logger log = LoggerFactory.getLogger(AniZipClient.class);
    private final WebClient animappingWebClient;

    public AniZipClient(WebClient animappingWebClient) {
        this.animappingWebClient = animappingWebClient;
    }

    /**
     * Hàm public: gọi API AniZip -> parse -> trả về list EpisodeMeta
     */
    public Mono<List<EpisodeMeta>> fetchEpisodeMeta(String anilistId) {
        return getAniZipResponse(anilistId)
                .map(this::extractEpisodesFromResponse)
                .onErrorMap(e -> new EpisodeFetchException("AniZip fetch failed for ID: " + anilistId, e));
    }

    /**
     * Gọi API AniZip, trả về toàn bộ JSON response
     */
    private Mono<JsonNode> getAniZipResponse(String anilistId) {
        String uri = "/?anilist_id=" + anilistId;
        return animappingWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnNext(json -> log.debug("Fetched AniZip response for {}: {}", anilistId, json))
                .onErrorResume(e -> {
                    log.error("Error fetching AniZip data for {}", anilistId, e);
                    return Mono.empty();
                });
    }

    /**
     * Trích danh sách các Episode từ JSON response
     */
    private List<EpisodeMeta> extractEpisodesFromResponse(JsonNode root) {
        if (root == null || !root.has("episodes")) {
            return List.of();
        }

        JsonNode episodesNode = root.path("episodes");
        List<EpisodeMeta> episodes = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = episodesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode episodeNode = entry.getValue();

            EpisodeMeta episodeMeta = buildEpisodeMeta(episodeNode);
            episodes.add(episodeMeta);
        }

        // Có thể sort theo số tập nếu cần
        episodes.sort(Comparator.comparing(EpisodeMeta::getEpisode));
        return episodes;
    }
    /**
     * Chuyển 1 episode JSON nhỏ thành EpisodeMeta object
     */
    private EpisodeMeta buildEpisodeMeta(JsonNode node) {
        String episodeNumber = node.path("episode").asText("");
        String summary = node.path("summary").asText("");
        String image = node.path("image").asText("");

        // Lấy title chính xác ưu tiên x-jat > en > fallback
        Map<String, String> titleMap = new HashMap<>();
        JsonNode titleNode = node.path("title");
        if (titleNode.isObject()) {
            titleNode.fieldNames().forEachRemaining(key -> {
                titleMap.put(key, titleNode.path(key).asText(""));
            });
        } else if (titleNode.isTextual()) {
            titleMap.put("default", titleNode.asText());
        }

        // Giới hạn chỉ giữ lại x-jat và en (nếu có)
        Map<String, String> filteredTitle = new LinkedHashMap<>();
        if (titleMap.containsKey("x-jat")) filteredTitle.put("x-jat", titleMap.get("x-jat"));
        if (titleMap.containsKey("en")) filteredTitle.put("en", titleMap.get("en"));
        if (filteredTitle.isEmpty() && !titleMap.isEmpty()) {
            // fallback nếu không có 2 key trên
            Map.Entry<String, String> first = titleMap.entrySet().iterator().next();
            filteredTitle.put(first.getKey(), first.getValue());
        }

        EpisodeMeta meta = new EpisodeMeta();
        meta.setEpisode(episodeNumber);
        meta.setSummary(summary);
        meta.setImage(image);
        meta.setTitle(filteredTitle);

        return meta;
    }
}