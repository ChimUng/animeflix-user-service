package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.request.AddFavoriteRequest;
import com.animeflix.userservice.dto.response.FavoriteResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.FavoriteService;
import com.animeflix.userservice.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<FavoriteResponse>>> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> favoriteService.addFavorite(userId, request))
                .map(response -> ResponseEntity.ok(ApiResponse.success(
                        "Added to favorites", response)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<FavoriteResponse>>>> getFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(userId -> favoriteService.getFavorites(userId, page, size))
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @GetMapping("/check/{animeId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Boolean>>>> checkFavorite(
            @PathVariable String animeId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> favoriteService.isFavorite(userId, animeId))
                .map(isFav -> ResponseEntity.ok(ApiResponse.success(
                        Map.of("isFavorite", isFav))));
    }

    @DeleteMapping("/{animeId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> removeFavorite(
            @PathVariable String animeId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> favoriteService.removeFavorite(userId, animeId))
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("Removed from favorites", null))));
    }

    @PutMapping("/{animeId}/notify")
    public Mono<ResponseEntity<ApiResponse<FavoriteResponse>>> toggleNotification(
            @PathVariable String animeId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> favoriteService.toggleNotification(userId, animeId))
                .map(response -> ResponseEntity.ok(ApiResponse.success(
                        "Notification settings updated", response)));
    }
}