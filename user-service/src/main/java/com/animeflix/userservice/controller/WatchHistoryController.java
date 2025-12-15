package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.request.AddHistoryRequest;
import com.animeflix.userservice.dto.response.WatchHistoryResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.WatchHistoryService;
import com.animeflix.userservice.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/user/history")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService historyService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<WatchHistoryResponse>>> addHistory(
            @Valid @RequestBody AddHistoryRequest request,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> historyService.addOrUpdateHistory(userId, request))
                .map(response -> ResponseEntity.ok(ApiResponse.success(
                        "Watch history updated successfully", response)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<WatchHistoryResponse>>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(userId -> historyService.getHistory(userId, page, size))
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @GetMapping("/anime/{aniId}")
    public Mono<ResponseEntity<ApiResponse<List<WatchHistoryResponse>>>> getHistoryByAnime(
            @PathVariable String aniId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(userId -> historyService.getHistoryByAnime(userId, aniId))
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @DeleteMapping("/anime/{aniId}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteByAnime(
            @PathVariable String aniId,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> historyService.deleteByAnime(userId, aniId))
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("History deleted successfully", null))));
    }

    @DeleteMapping
    public Mono<ResponseEntity<ApiResponse<Void>>> clearHistory(ServerWebExchange exchange) {
        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(historyService::clearHistory)
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("All history cleared", null))));
    }
}
