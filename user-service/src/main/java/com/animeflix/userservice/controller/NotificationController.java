package com.animeflix.userservice.controller;

import com.animeflix.userservice.dto.response.NotificationResponse;
import com.animeflix.userservice.exception.ApiResponse;
import com.animeflix.userservice.service.NotificationService;
import com.animeflix.userservice.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<NotificationResponse>>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(userId -> notificationService.getNotifications(userId, page, size))
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @GetMapping("/unread")
    public Mono<ResponseEntity<ApiResponse<List<NotificationResponse>>>> getUnreadNotifications(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMapMany(notificationService::getUnreadNotifications)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.success(list)));
    }

    @GetMapping("/unread-count")
    public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> getUnreadCount(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(notificationService::countUnread)
                .map(count -> ResponseEntity.ok(ApiResponse.success(
                        Map.of("count", count))));
    }

    @PutMapping("/{id}/read")
    public Mono<ResponseEntity<ApiResponse<NotificationResponse>>> markAsRead(
            @PathVariable String id,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> notificationService.markAsRead(userId, id))
                .map(response -> ResponseEntity.ok(ApiResponse.success(
                        "Marked as read", response)));
    }

    @PutMapping("/read-all")
    public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> markAllAsRead(
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(notificationService::markAllAsRead)
                .map(count -> ResponseEntity.ok(ApiResponse.success(
                        "All notifications marked as read",
                        Map.of("updatedCount", count))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteNotification(
            @PathVariable String id,
            ServerWebExchange exchange) {

        return SecurityContextUtil.getCurrentUserId(exchange)
                .flatMap(userId -> notificationService.deleteNotification(userId, id))
                .then(Mono.just(ResponseEntity.ok(
                        ApiResponse.success("Notification deleted", null))));
    }
}