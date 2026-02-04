package com.animeflix.animeepisode.controller.stream;

import com.animeflix.animeepisode.model.stream.VideoData;
import com.animeflix.animeepisode.service.stream.VideoStreamService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 🎬 StreamController - NEW CONTROLLER với routing rõ ràng
 *
 * ✅ Design theo Consumet API pattern:
 * - Mỗi provider có endpoint riêng
 * - Path parameters rõ ràng (anilistId, episodeId)
 * - Request body đơn giản, không cần field "source"/"provider"
 *
 * ❌ DEPRECATE VideoStreamController (POST /api/streams/{id})
 */
@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StreamController {

    private final VideoStreamService streamService;

    // ========================================
    // CONSUMET PROVIDERS (Gogoanime, Gogobackup)
    // ========================================

    /**
     * POST /api/stream/consumet/{episodeId}
     *
     * Body: { "provider": "gogoanime" | "gogobackup" }
     *
     * Example:
     * POST /api/stream/consumet/one-piece-episode-1
     * { "provider": "gogoanime" }
     */
    @PostMapping("/consumet/{episodeId}")
    public Mono<ResponseEntity<VideoData>> getConsumetStream(
            @PathVariable String episodeId,
            @RequestBody(required = false) ConsumetRequest request,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        String provider = request != null ? request.getProvider() : "gogoanime";
        log.info("📥 POST /api/stream/consumet/{} - provider={}, refresh={}",
                episodeId, provider, refresh);

        return streamService.fetchConsumetStream(episodeId, refresh)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ Consumet stream error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ========================================
    // ZORO PROVIDER
    // ========================================

    /**
     * POST /api/stream/zoro/{anilistId}
     *
     * Body: { "episodeId": "3303", "subtype": "sub" | "dub" }
     *
     * Example:
     * POST /api/stream/zoro/21
     * { "episodeId": "3303", "subtype": "sub" }
     */
    @PostMapping("/zoro/{anilistId}")
    public Mono<ResponseEntity<VideoData>> getZoroStream(
            @PathVariable String anilistId,
            @RequestBody ZoroRequest request,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 POST /api/stream/zoro/{} - request={}, refresh={}",
                anilistId, request, refresh);

        String subtype = request.getSubtype() != null ? request.getSubtype() : "sub";

        return streamService.fetchZoroStream(
                        anilistId,
                        request.getEpisodeId(),
                        subtype,
                        refresh
                )
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ Zoro stream error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ========================================
    // 9ANIME PROVIDER
    // ========================================

    /**
     * POST /api/stream/9anime/{anilistId}
     *
     * Body: { "episodeId": "3303", "subtype": "sub" | "dub" }
     *
     * Example:
     * POST /api/stream/9anime/21
     * { "episodeId": "3303", "subtype": "sub" }
     */
    @PostMapping("/9anime/{anilistId}")
    public Mono<ResponseEntity<VideoData>> get9AnimeStream(
            @PathVariable String anilistId,
            @RequestBody NineAnimeRequest request,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 POST /api/stream/9anime/{} - request={}, refresh={}",
                anilistId, request, refresh);

        String subtype = request.getSubtype() != null ? request.getSubtype() : "sub";

        return streamService.fetch9AnimeStream(
                        anilistId,
                        request.getEpisodeId(),
                        subtype,
                        refresh
                )
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ 9anime stream error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ========================================
    // ANIMEPAHE PROVIDER
    // ========================================

    /**
     * POST /api/stream/animepahe/{episodeId}
     *
     * No body required (AnimePahe chỉ cần episodeId)
     *
     * Example:
     * POST /api/stream/animepahe/d58fc9f8.../f3316203...
     */
    @PostMapping("/animepahe/{episodeId}")
    public Mono<ResponseEntity<VideoData>> getAnimePaheStream(
            @PathVariable String episodeId,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 POST /api/stream/animepahe/{} - refresh={}", episodeId, refresh);

        return streamService.fetchAnimePaheStream(episodeId, refresh)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ AnimePahe stream error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ========================================
    // ANIFY PROVIDER (Generic fallback)
    // ========================================

    /**
     * POST /api/stream/anify/{anilistId}
     *
     * Body: { "provider": "zoro" | "gogoanime" | ..., "episodeId": "...", "episodeNum": "1", "subtype": "sub" }
     *
     * Example:
     * POST /api/stream/anify/21
     * { "provider": "zoro", "episodeId": "3303", "episodeNum": "1", "subtype": "sub" }
     */
    @PostMapping("/anify/{anilistId}")
    public Mono<ResponseEntity<VideoData>> getAnifyStream(
            @PathVariable String anilistId,
            @RequestBody AnifyRequest request,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 POST /api/stream/anify/{} - request={}, refresh={}",
                anilistId, request, refresh);

        String subtype = request.getSubtype() != null ? request.getSubtype() : "sub";

        return streamService.fetchAnifyStream(
                        request.getProvider(),
                        request.getEpisodeId(),
                        request.getEpisodeNum(),
                        anilistId,
                        subtype,
                        refresh
                )
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ Anify stream error: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // ========================================
    // REQUEST DTOs
    // ========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumetRequest {
        private String provider; // "gogoanime" | "gogobackup"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoroRequest {
        private String episodeId; // Episode ID (số hoặc đã build)
        private String subtype;   // "sub" | "dub"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NineAnimeRequest {
        private String episodeId;
        private String subtype;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnifyRequest {
        private String provider;   // "zoro" | "gogoanime" | ...
        private String episodeId;
        private String episodeNum; // Episode number
        private String subtype;
    }
}