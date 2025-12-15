package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.response.ContinueWatchingResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.ContinueWatchingService;
import com.animeflix.userservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/user/continue-watching")
@RequiredArgsConstructor
public class ContinueWatchingController {

    private final ContinueWatchingService continueWatchingService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ContinueWatchingResponse>>>> getContinueWatching(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(continueWatchingService::getContinueWatching)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @DeleteMapping("/{aniId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> removeFromContinueWatching(
            @PathVariable String aniId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> continueWatchingService.removeFromContinueWatching(userId, aniId))
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("Removed from continue watching", null))));
    }
}
