package com.animeflix.animeepisode.controller;
import com.animeflix.animeepisode.exception.EpisodeFetchException;
import com.animeflix.animeepisode.model.EpisodeResponse;
import com.animeflix.animeepisode.service.EpisodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/episodes")
@CrossOrigin(origins = "*")
public class EpisodeController {
    private final EpisodeService episodeService;

    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    /**
     * Lấy danh sách tập phim theo ID anime.
     * Ví dụ: GET /api/episodes/20605?releasing=true&refresh=false
     */
    @GetMapping("/{animeId}")
    public Mono<ResponseEntity<EpisodeResponse>> getEpisodes(
            @PathVariable String animeId,
            @RequestParam(defaultValue = "false") boolean releasing,
            @RequestParam(defaultValue = "false") boolean refresh) {
        // Validation for animeId: must be a positive integer
        long idValue;
        try {
            idValue = Long.parseLong(animeId);
            if (idValue <= 0) {
                return Mono.just(ResponseEntity.badRequest().build());
            }
        } catch (NumberFormatException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return episodeService.fetchEpisodes(animeId, releasing, refresh)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(EpisodeFetchException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }

    /**
     * Endpoint test để ép refetch dữ liệu và làm mới cache.
     * Ví dụ: POST /api/episodes/20605/refresh
     */
    @PostMapping("/{animeId}/refresh")
    public Mono<ResponseEntity<EpisodeResponse>> refreshEpisodes(@PathVariable String animeId) {
        // Validation for animeId
        long idValue;
        try {
            idValue = Long.parseLong(animeId);
            if (idValue <= 0) {
                return Mono.just(ResponseEntity.badRequest().build());
            }
        } catch (NumberFormatException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return episodeService.fetchEpisodes(animeId, false, true)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .onErrorResume(EpisodeFetchException.class, e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }

    @ExceptionHandler(EpisodeFetchException.class)
    public ResponseEntity<String> handleEpisodeFetchException(EpisodeFetchException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
