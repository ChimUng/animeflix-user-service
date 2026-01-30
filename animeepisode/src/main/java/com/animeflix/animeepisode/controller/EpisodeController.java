package com.animeflix.animeepisode.controller;

import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.Provider;
import com.animeflix.animeepisode.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/episode")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class EpisodeController {

    private final EpisodeService episodeService;

    /**
     * ✅ GET /api/episode/{id}?title=...&year=...&type=...&releasing=...&refresh=...
     *
     * Response: List<Provider> (KHÔNG có wrapper để khớp Next.js)
     */
    @GetMapping("/{animeId}")
    public Mono<ResponseEntity<List<Provider>>> getEpisodes(
            @PathVariable String animeId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean releasing,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        log.info("📥 GET /api/episode/{} - title={}, year={}, type={}, releasing={}, refresh={}",
                animeId, title, year, type, releasing, refresh);

        // Validation
        try {
            long idValue = Long.parseLong(animeId);
            if (idValue <= 0) {
                log.warn("⚠️ Invalid animeId: {}", animeId);
                return Mono.just(ResponseEntity.badRequest().build());
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ Invalid animeId format: {}", animeId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return episodeService.fetchEpisodes(animeId, title, year, type, releasing, refresh)
                .doOnSuccess(providers -> log.info("✅ Returned {} providers for anime {}",
                        providers.size(), animeId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(EpisodeFetchException.class, e -> {
                    log.error("❌ Error fetching episodes for {}: {}", animeId, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * ✅ POST /api/episode/{id}/refresh - Force refresh
     */
    @PostMapping("/{animeId}/refresh")
    public Mono<ResponseEntity<List<Provider>>> refreshEpisodes(
            @PathVariable String animeId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type
    ) {
        log.info("🔄 POST /api/episode/{}/refresh", animeId);
        return getEpisodes(animeId, title, year, type, false, true);
    }
}