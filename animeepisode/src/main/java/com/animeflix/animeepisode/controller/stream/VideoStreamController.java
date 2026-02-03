package com.animeflix.animeepisode.controller.stream;

import com.animeflix.animeepisode.stream.service.VideoStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Stream endpoint.
 * POST /api/streams/{id}
 *
 * Body: { source, provider, episodeid, episodenum, subtype }
 * Response: VideoData | error
 */
@RestController
@RequestMapping("/api/streams")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class VideoStreamController {

    private final VideoStreamService videoStreamService;

    @PostMapping("/{id}")
    public Mono<ResponseEntity<VideoData>> getStream(
            @PathVariable String id,
            @RequestBody StreamRequest request,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 POST /api/streams/{} - request={}, refresh={}", id, request, refresh);

        if (id == null || id.isEmpty()) {
            log.warn("⚠️ Invalid anime ID");
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return videoStreamService.fetchStream(id, request, refresh)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .onErrorResume(e -> {
                    log.error("❌ Stream error for {}: {}", id, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}






