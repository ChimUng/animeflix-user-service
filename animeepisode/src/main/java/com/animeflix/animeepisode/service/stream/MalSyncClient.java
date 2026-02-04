package com.animeflix.animeepisode.service.stream;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.MalSyncEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MalSyncClient {

    private final WebClient webClient;

    public MalSyncClient(WebClient malsyncWebClient) {
        this.webClient = malsyncWebClient;
    }

    /**
     * 🆕 NEW METHOD - Lấy Zoro/Hianime slug từ MalSync
     * Giờ trả về Mono<String> (slug hoặc null) — giống Next.js
     */
    public Mono<String> getZoroSlug(String id) {
        log.debug("🔍 MalSync: Fetching Zoro slug for ID: {}", id);

        return webClient.get()
                .uri("/" + id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(data -> {
                    if (data == null || !data.containsKey("Sites")) {
                        log.warn("⚠️ MalSync: No Sites data for ID: {}", id);
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> sites = (Map<String, Object>) data.get("Sites");

                    // Tìm site "Zoro" (case-insensitive)
                    Map.Entry<String, Object> zoroEntry = sites.entrySet().stream()
                            .filter(entry -> entry.getKey().equalsIgnoreCase("zoro"))
                            .findFirst()
                            .orElse(null);

                    if (zoroEntry == null) {
                        log.warn("⚠️ MalSync: No Zoro site for ID: {}", id);
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> zoroData = (Map<String, Object>) zoroEntry.getValue();

                    Object firstEntry = zoroData.values().stream()
                            .filter(Map.class::isInstance)
                            .findFirst()
                            .orElse(null);

                    if (firstEntry == null) {
                        log.warn("⚠️ MalSync: No Zoro entry for ID: {}", id);
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> entryData = (Map<String, Object>) firstEntry;
                    String rawUrl = (String) entryData.get("url");

                    if (rawUrl == null || rawUrl.isEmpty()) {
                        log.warn("⚠️ MalSync: No URL in Zoro entry for ID: {}", id);
                        return null;
                    }

                    // Extract slug
                    String slug = rawUrl
                            .replaceAll("^https?://(www\\.)?hianime\\.to/", "")
                            .replaceAll("^/|/$", "");

                    if (slug.isEmpty()) {
                        log.warn("⚠️ MalSync: Empty slug for ID: {}", id);
                        return null;
                    }

                    log.info("✅ MalSync: Found Zoro slug: {} for ID: {}", slug, id);
                    return slug;
                })
                .onErrorResume(e -> {
                    log.error("❌ MalSync: Error fetching slug for ID {}: {}", id, e.getMessage());
                    return Mono.just((String) null);
                });
    }
}