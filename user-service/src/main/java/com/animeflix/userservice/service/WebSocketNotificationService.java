package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.websocket.WebSocketNotificationMessage;
import com.animeflix.userservice.entity.Notification;
import com.animeflix.userservice.handler.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

/**
 * ✅ WebSocket Notification Service (Updated for Reactive WebSocket)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final NotificationWebSocketHandler webSocketHandler;

    /**
     * Send notification to specific user via WebSocket
     * Only sends if user is online
     */
    public void sendToUser(String userId, Notification notification) {
        webSocketHandler.isUserOnline(userId)
                .subscribe(online -> {
                    if (Boolean.TRUE.equals(online)) {
                        WebSocketNotificationMessage message = buildMessage(notification);
                        webSocketHandler.sendToUser(userId, message);
                    } else {
                        log.debug("⏭️ User {} offline, notification stored only", userId);
                    }
                });
    }

    /**
     * Broadcast notification to all online users
     */
    public void broadcast(WebSocketNotificationMessage message) {
        webSocketHandler.broadcast(message);
    }

    /**
     * Get count of online users
     */
    public Mono<Long> getOnlineUsersCount() {
        return webSocketHandler.getOnlineUsersCount();
    }

    /**
     * Build WebSocket message from notification entity
     */
    private WebSocketNotificationMessage buildMessage(Notification notification) {
        return WebSocketNotificationMessage.builder()
                .type(notification.getType().name())
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .imageUrl(notification.getImageUrl())
                .animeId(notification.getAnimeId())
                .episodeNumber(notification.getEpisodeNumber())
                .actionUrl(notification.getActionUrl())
                .timestamp(notification.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli())
                .priority("NORMAL")
                .build();
    }
}