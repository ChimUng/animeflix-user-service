package com.animeflix.animeinfo.controller;

import com.animeflix.animeinfo.service.AnimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getAnimeInfo(@PathVariable String id) {
        return animeService.getAnimeInfo(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching anime info: " + e.getMessage())));
    }

    @GetMapping("/favourite")
    public Mono<ResponseEntity<String>> getFavouriteAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        return animeService.getFavouriteAnime(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching favourite anime: " + e.getMessage())));
    }

    @GetMapping("/schedule")
    public Mono<ResponseEntity<String>> getAnimeSchedule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(defaultValue = "0") long airingAtGreater) {
        return animeService.getAnimeSchedule(page, perPage, airingAtGreater)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching anime schedule: " + e.getMessage())));
    }

    @GetMapping("/season")
    public Mono<ResponseEntity<String>> getAnimeSeason(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        return animeService.getSeasonAnime(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching season anime: " + e.getMessage())));
    }

    @GetMapping("/popularmovie")
    public Mono<ResponseEntity<String>> getPopularMovie(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        return animeService.getPopularMovie(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching popular movies: " + e.getMessage())));
    }

    @GetMapping("/popularthisseason")
    public Mono<ResponseEntity<String>> getPopularThisSeason(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int perPage) {
        return animeService.getPopularThisSeason(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching popular this season: " + e.getMessage())));
    }

    @GetMapping("/popularnextseason")
    public Mono<ResponseEntity<String>> getPopularNextSeason(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int perPage) {
        return animeService.getPopularNextSeason(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching popular next season: " + e.getMessage())));
    }

    @GetMapping("/top100")
    public Mono<ResponseEntity<String>> getTop100Anime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        return animeService.getTop100Anime(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching top 100 anime: " + e.getMessage())));
    }

    @GetMapping("/popular")
    public Mono<ResponseEntity<String>> getPopularAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int perPage) {
        return animeService.getPopularAnime(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching popular anime: " + e.getMessage())));
    }

    @GetMapping("/trending")
    public Mono<ResponseEntity<String>> getTrendingAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int perPage) {
        return animeService.getTrendingAnime(page, perPage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error fetching trending anime: " + e.getMessage())));
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<String>> searchAnime(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> genres, // Expect genres as a flat list, e.g., ["Action", "Drama"]
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page) {
        // Convert flat genres list to List<Map<String, String>> for service
        List<Map<String, String>> genreList = genres != null && !genres.isEmpty()
                ? genres.stream()
                .map(genre -> Map.of("type", "genre", "value", genre))
                .collect(Collectors.toList())
                : null;

        return animeService.searchAnime(search, year, season, format, genreList, sort, page)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body("Error searching anime: " + e.getMessage())));
    }
}