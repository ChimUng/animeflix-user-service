package com.animeflix.animerecommendation.controller;

import com.animeflix.animerecommendation.model.ScoredRecentEpisode;
import com.animeflix.animerecommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/recommends")
public class AppController {

    private final RecommendationService recommendationService;

    public AppController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{username}")
    public Mono<List<ScoredRecentEpisode>> getRecommendationsByUser(@PathVariable String username) {
        System.out.println(">>> Gọi recommend cho user: " + username);
        return recommendationService.getRecommendations(username)
                .doOnNext(list -> System.out.println(">>> Có " + list.size() + " gợi ý"))
                .doOnError(err -> err.printStackTrace());
    }
}
