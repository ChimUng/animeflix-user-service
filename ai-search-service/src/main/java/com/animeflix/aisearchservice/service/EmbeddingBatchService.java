package com.animeflix.aisearchservice.service;

import com.animeflix.aisearchservice.dto.kafka.AnimeUpdatedEvent;
import com.animeflix.aisearchservice.Entity.AnimeVector;
import com.animeflix.aisearchservice.Repository.AnimeVectorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch Job: Embed toàn bộ anime một lần duy nhất khi setup hệ thống.
 *
 * Trigger thủ công qua API: POST /api/admin/embedding/batch
 * KHÔNG tự động chạy khi startup (batch.embedding.enabled = false)
 *
 * Flow:
 * 1. Lấy anime chưa embed từ anime_vectors collection
 *    (hoặc fetch từ catalog service nếu collection rỗng)
 * 2. Chia thành batch nhỏ (50 anime / batch)
 * 3. Embed từng batch, có delay giữa các batch (tránh rate limit Gemini)
 * 4. Upsert vào anime_vectors
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingBatchService {

    private final AnimeVectorRepository animeVectorRepository;
    private final EmbeddingService embeddingService;
    private final WebClient catalogWebClient;

    @Value("${search.embedding.batch-size:50}")
    private int batchSize;

    @Value("${search.embedding.rate-limit-delay-ms:200}")
    private long rateLimitDelayMs;

    private volatile boolean isRunning = false;

    /**
     * Chạy batch embed toàn bộ.
     * Lấy anime chưa embed từ MongoDB → embed → save.
     */
    public Mono<String> runBatch() {
        if (isRunning) {
            return Mono.just("⚠️ Batch đang chạy, không thể chạy song song");
        }

        return animeVectorRepository.countByEmbeddedFalseOrEmbeddedIsNull()
                .flatMap(count -> {
                    if (count == 0) {
                        log.info("✅ Tất cả anime đã được embed");
                        return Mono.just("✅ Không có anime nào cần embed");
                    }

                    log.info("🚀 Bắt đầu batch embed {} anime...", count);
                    isRunning = true;

                    return animeVectorRepository
                            .findByEmbeddedFalseOrEmbeddedIsNull(
                                    PageRequest.of(0, (int) (long) count))
                            .buffer(batchSize)  // Chia thành batch 50
                            .concatMap(batch -> processBatch(batch)
                                    .delayElement(Duration.ofMillis(rateLimitDelayMs)))
                            .doOnComplete(() -> {
                                isRunning = false;
                                log.info("🎉 Batch embed hoàn thành!");
                            })
                            .doOnError(e -> {
                                isRunning = false;
                                log.error("❌ Batch embed lỗi: {}", e.getMessage());
                            })
                            .then(Mono.just("✅ Batch embed hoàn thành cho " + count + " anime"));
                });
    }

    /**
     * Fetch từ catalog service và seed vào anime_vectors (chưa embed).
     * Dùng khi anime_vectors collection rỗng hoàn toàn.
     */
    public Mono<String> seedFromCatalog(int page, int perPage) {
        log.info("📥 Seeding anime data từ catalog service, page={}", page);

        return catalogWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/popular")
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    JsonNode dataList = response.path("data");
                    List<AnimeVector> newVectors = new ArrayList<>();

                    if (dataList.isArray()) {
                        dataList.elements().forEachRemaining(node -> {
                            String id = node.path("id").asText();
                            // Tạo document với embedded=false → batch job sẽ embed sau
                            AnimeVector av = AnimeVector.builder()
                                    .id(id)
                                    .titleRomaji(node.path("title").path("romaji").asText(null))
                                    .titleEnglish(node.path("title").path("english").asText(null))
                                    .description(node.path("description").asText(null))
                                    .genres(parseStringList(node.path("genres")))
                                    .coverImageLarge(node.path("coverImage").path("large").asText(null))
                                    .averageScore(node.path("averageScore").asInt(0))
                                    .popularity(node.path("popularity").asInt(0))
                                    .status(node.path("status").asText(null))
                                    .format(node.path("format").asText(null))
                                    .season(node.path("season").asText(null))
                                    .seasonYear(node.path("seasonYear").asInt(0))
                                    .embedded(false)   // Chưa embed
                                    .build();
                            newVectors.add(av);
                        });
                    }

                    return animeVectorRepository.saveAll(newVectors).collectList()
                            .map(saved -> "✅ Seeded " + saved.size() + " anime từ catalog (page " + page + ")");
                });
    }

    public boolean isRunning() {
        return isRunning;
    }

    private Mono<Void> processBatch(List<AnimeVector> batch) {
        log.info("⚙️ Processing batch of {} anime...", batch.size());

        return Flux.fromIterable(batch)
                .concatMap(av -> {
                    // Tạo event tạm để gọi EmbeddingService
                    AnimeUpdatedEvent event = AnimeUpdatedEvent.builder()
                            .animeId(av.getId())
                            .titleRomaji(av.getTitleRomaji())
                            .titleEnglish(av.getTitleEnglish())
                            .description(av.getDescription())
                            .genres(av.getGenres())
                            .tags(av.getTags())
                            .coverImageLarge(av.getCoverImageLarge())
                            .averageScore(av.getAverageScore())
                            .popularity(av.getPopularity())
                            .status(av.getStatus())
                            .format(av.getFormat())
                            .season(av.getSeason())
                            .seasonYear(av.getSeasonYear())
                            .build();

                    return embeddingService.embedAndSave(event)
                            .onErrorResume(e -> {
                                log.error("❌ Skip anime {} do lỗi: {}", av.getId(), e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            node.elements().forEachRemaining(n -> list.add(n.asText()));
        }
        return list;
    }
}