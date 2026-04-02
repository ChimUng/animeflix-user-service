package com.animeflix.aisearchservice.controller;

import com.animeflix.aisearchservice.exception.ApiResponse;
import com.animeflix.aisearchservice.Repository.AnimeVectorRepository;
import com.animeflix.aisearchservice.service.EmbeddingBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Admin endpoints để quản lý embedding.
 * Nên bảo vệ bằng API key hoặc internal network trong production.
 */
@RestController
@RequestMapping("/api/admin/embedding")
@RequiredArgsConstructor
public class EmbeddingAdminController {

    private final EmbeddingBatchService embeddingBatchService;
    private final AnimeVectorRepository animeVectorRepository;

    /**
     * Trigger batch embed toàn bộ anime chưa embed.
     * POST /api/admin/embedding/batch
     */
    @PostMapping("/batch")
    public Mono<ResponseEntity<ApiResponse<String>>> runBatch() {
        return embeddingBatchService.runBatch()
                .map(msg -> ResponseEntity.ok(ApiResponse.ok(msg)));
    }

    /**
     * Seed data từ catalog service trước khi batch embed.
     * POST /api/admin/embedding/seed?page=1&perPage=50
     */
    @PostMapping("/seed")
    public Mono<ResponseEntity<ApiResponse<String>>> seedFromCatalog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int perPage) {
        return embeddingBatchService.seedFromCatalog(page, perPage)
                .map(msg -> ResponseEntity.ok(ApiResponse.ok(msg)));
    }

    /**
     * Kiểm tra status embedding.
     * GET /api/admin/embedding/status
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<ApiResponse<Object>>> getStatus() {
        return Mono.zip(
                animeVectorRepository.count(),
                animeVectorRepository.countByEmbeddedTrue(),
                animeVectorRepository.countByEmbeddedFalseOrEmbeddedIsNull()
        ).map(tuple -> {
            var status = java.util.Map.of(
                    "totalDocuments", tuple.getT1(),
                    "embedded", tuple.getT2(),
                    "pendingEmbed", tuple.getT3(),
                    "batchRunning", embeddingBatchService.isRunning()
            );
            return ResponseEntity.ok(ApiResponse.ok((Object) status));
        });
    }
}