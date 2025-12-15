package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.response.UserStatsResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.WatchHistoryService;
import com.animeflix.userservice.service.FavoriteService;
import com.animeflix.userservice.service.NotificationService;
import com.animeflix.userservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user/stats")
@RequiredArgsConstructor
public class UserStatsController {

    private final WatchHistoryService historyService;
    private final FavoriteService favoriteService;
    private final NotificationService notificationService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<UserStatsResponse>>> getUserStats(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> {
                    Mono<Long> animeWatched = historyService.countAnimeWatched(userId);
                    Mono<Long> totalSeconds = historyService.getTotalWatchedSeconds(userId);
                    Mono<Long> favCount = favoriteService.countFavorites(userId);
                    Mono<Long> unreadNoti = notificationService.countUnread(userId);

                    return Mono.zip(animeWatched, totalSeconds, favCount, unreadNoti)
                            .map(tuple -> UserStatsResponse.builder()
                                    .totalAnimeWatched(tuple.getT1())
                                    .totalWatchTimeSeconds(tuple.getT2())
                                    .favoritesCount(tuple.getT3())
                                    .unreadNotifications(tuple.getT4())
                                    .build());
                })
                .map(stats -> ResponseEntity.ok(ApiResponse.success(stats)));
    }
}