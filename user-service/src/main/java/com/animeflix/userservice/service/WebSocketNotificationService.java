package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.websocket.WebSocketNotificationMessage;
import com.animeflix.userservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String ONLINE_USERS_KEY = "online:users";

    /**
     * Send notification to specific user via WebSocket
     * Only sends if user is online
     */
    public void sendToUser(String userId, Notification notification) {
        // Check if user online
        isUserOnline(userId)
                .subscribe(online -> {
                    if (Boolean.TRUE.equals(online)) {
                        WebSocketNotificationMessage message = buildMessage(notification);

                        // Send to user-specific queue
                        messagingTemplate.convertAndSendToUser(
                                userId,
                                "/queue/notifications",
                                message
                        );

                        log.info("📲 Notification sent to user {} via WebSocket", userId);
                    } else {
                        log.debug("⏭️ User {} offline, notification stored only", userId);
                    }
                });
    }

    /**
     * Broadcast notification to all online users
     */
    public void broadcast(WebSocketNotificationMessage message) {
        messagingTemplate.convertAndSend("/topic/notifications", message);
        log.info("📢 Notification broadcast to all users");
    }

    /**
     * Mark user as online (called when user connects)
     */
    public void markUserOnline(String userId) {
        redisTemplate.opsForSet()
                .add(ONLINE_USERS_KEY, userId)
                .subscribe(added -> {
                    if (Boolean.TRUE.equals(added)) {
                        log.info("👤 User {} marked as online", userId);
                    }
                });
    }

    /**
     * Mark user as offline (called when user disconnects)
     */
    public void markUserOffline(String userId) {
        redisTemplate.opsForSet()
                .remove(ONLINE_USERS_KEY, userId)
                .subscribe(removed -> {
                    if (removed > 0) {
                        log.info("👤 User {} marked as offline", userId);
                    }
                });
    }

    /**
     * Check if user is online
     */
    private reactor.core.publisher.Mono<Boolean> isUserOnline(String userId) {
        return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
    }

    /**
     * Get count of online users
     */
    public reactor.core.publisher.Mono<Long> getOnlineUsersCount() {
        return redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
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