package com.animeflix.animecatalogservice.controller;

import com.animeflix.animecatalogservice.DTO.AnimeDetailResponse;
import com.animeflix.animecatalogservice.DTO.AnimeResponse;
import com.animeflix.animecatalogservice.exception.ApiResponse;
import com.animeflix.animecatalogservice.exception.NotFoundException;
import com.animeflix.animecatalogservice.service.AnimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnimeController {

    private final AnimeService animeService;

    // 1. Chi tiết anime
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<AnimeDetailResponse>>> getAnimeInfo(@PathVariable String id) {
        return animeService.getAnimeInfo(id)
                .map(anime -> ResponseEntity.ok(ApiResponse.ok(anime)))
                .switchIfEmpty(Mono.error(new NotFoundException("Anime không tồn tại hoặc đang được cập nhật")))
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(404)
                        .body(ApiResponse.error(ex.getMessage()))));
    }

    // 2. Top 100
    @GetMapping("/top100")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getTop100Anime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getTop100Anime(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 3. Trending(recent anime)
    @GetMapping("/trending")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getTrendingAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getTrendingAnime(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 4. Phim lẻ
    @GetMapping("/popularmovie")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getPopularMovie(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getPopularMovies(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 5. Mùa hiện tại
    @GetMapping("/season")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getCurrentSeason(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getCurrentSeasonAnime(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 6. Popular Anime
    @GetMapping("/popular")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getPopularAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getPopularAnime(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 7. Next Season Anime
    @GetMapping("/nextseason")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> getNextSeasonAnime(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return animeService.getNextSeasonAnime(page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 8. Anime Schedule
    @GetMapping("/schedule")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAnimeSchedule(
            @RequestParam(defaultValue = "0") long airingAtGreater) {
        return animeService.getAnimeSchedule(airingAtGreater)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)));
    }

    // 9. Tìm kiếm
    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<List<AnimeResponse>>>> searchAnime(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(defaultValue = "POPULARITY_DESC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage) {

        return animeService.searchAnime(search, year, season, format, genres, sort, page, perPage)
                .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Error searching anime: " + e.getMessage()))));
    }
}