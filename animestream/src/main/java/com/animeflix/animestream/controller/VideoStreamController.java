package com.animeflix.animestream.controller;

import com.animeflix.animestream.exception.StreamFetchException;
import com.animeflix.animestream.Model.StreamRequest;
import com.animeflix.animestream.Model.StreamResponse;
import com.animeflix.animestream.service.VideoStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller to handle video stream requests.
 */
@RestController
@RequestMapping("/api/streams")
@CrossOrigin(origins = "*")
public class VideoStreamController {

    private static final Logger log = LoggerFactory.getLogger(VideoStreamController.class);
    private final VideoStreamService videoStreamService;

    public VideoStreamController(VideoStreamService videoStreamService) {
        this.videoStreamService = videoStreamService;
    }

    /**
     * Fetch video stream for a given anime ID.
     * @param id Anime ID (e.g., "21").
     * @param request StreamRequest with source, provider, episodeId, episodeNum, subtype.
     * @param refresh Force refresh data, bypassing cache.
     * @return ResponseEntity with StreamResponse or error.
     */
    @PostMapping("/{id}")
    public Mono<ResponseEntity<StreamResponse>> getStream(
            @PathVariable String id,
            @RequestBody StreamRequest request,
            @RequestParam(defaultValue = "false") boolean refresh) {
        log.info("Received stream request for ID: {}, request: {}, refresh: {}", id, request, refresh);

        if (id == null || id.isEmpty()) {
            log.warn("Invalid anime ID: {}", id);
            return Mono.just(ResponseEntity.badRequest().body(new StreamResponse(false, null)));
        }

        return videoStreamService.fetchStream(id, request, refresh)
                .map(response -> {
                    if (!response.isSuccess() || response.getData() == null) {
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
                    }
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(new StreamResponse(false, null)))
                .onErrorResume(StreamFetchException.class, e -> {
                    log.error("Stream fetch error for ID: {}", id, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new StreamResponse(false, null)));
                });
    }

    /**
     * Handle StreamFetchException globally.
     */
    @ExceptionHandler(StreamFetchException.class)
    public ResponseEntity<StreamResponse> handleStreamFetchException(StreamFetchException e) {
        log.error("Stream fetch exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new StreamResponse(false, null));
    }
}