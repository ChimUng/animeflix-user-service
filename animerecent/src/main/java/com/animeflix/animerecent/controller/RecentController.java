package com.animeflix.animerecent.controller;

import com.animeflix.animerecent.service.RecentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/anime")
public class RecentController {

    private final RecentService recentService;

    public RecentController(RecentService recentService) {
        this.recentService = recentService;
    }

    @GetMapping("/recent")
    public Mono<ResponseEntity<String>> getRecentEpisodes() {
        return recentService.getRecentEpisodes()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching recent episodes: " + e.getMessage())));
    }
}
