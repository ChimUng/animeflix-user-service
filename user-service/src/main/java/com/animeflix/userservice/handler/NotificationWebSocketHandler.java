package com.animeflix.userservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ Reactive WebSocket Handler
 *
 * Handles WebSocket connections for real-time notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Store active sessions: userId -> WebSocketSession
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    private static final String ONLINE_USERS_KEY = "online:users";

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = extractUserId(session);

        if (userId == null) {
            log.warn("❌ No userId found in WebSocket handshake");
            return session.close();
        }

        log.info("👤 User {} connected via WebSocket", userId);

        // Store session
        activeSessions.put(userId, session);

        // Mark user as online in Redis
        return markUserOnline(userId)
                .then(session.receive()
                        .doOnNext(msg -> handleMessage(session, userId, msg))
                        .then())
                .doFinally(signalType -> {
                    log.info("👤 User {} disconnected", userId);
                    activeSessions.remove(userId);
                    markUserOffline(userId).subscribe();
                });
    }

    /**
     * Extract userId from WebSocket handshake query params
     * Example: ws://localhost:8081/ws/notifications?userId=123
     */
    private String extractUserId(WebSocketSession session) {
        try {
            String query = session.getHandshakeInfo().getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                return query.split("userId=")[1].split("&")[0];
            }
        } catch (Exception e) {
            log.error("Failed to extract userId: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Handle incoming messages from client
     */
    private void handleMessage(WebSocketSession session, String userId, WebSocketMessage message) {
        String payload = message.getPayloadAsText();
        log.debug("📨 Received message from {}: {}", userId, payload);

        // Handle ping/pong or other client messages
        if ("ping".equals(payload)) {
            sendToSession(session, "pong");
        }
    }

    /**
     * Send message to specific user
     */
    public void sendToUser(String userId, Object message) {
        WebSocketSession session = activeSessions.get(userId);

        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                sendToSession(session, json);
                log.info("📲 Notification sent to user {} via WebSocket", userId);
            } catch (Exception e) {
                log.error("❌ Failed to send message to user {}: {}", userId, e.getMessage());
            }
        } else {
            log.debug("⏭️ User {} offline, notification stored only", userId);
        }
    }

    /**
     * Send message to WebSocket session
     */
    private void sendToSession(WebSocketSession session, String message) {
        session.send(Mono.just(session.textMessage(message))).subscribe();
    }

    /**
     * Broadcast message to all connected users
     */
    public void broadcast(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            activeSessions.values().forEach(session -> {
                if (session.isOpen()) {
                    sendToSession(session, json);
                }
            });
            log.info("📢 Notification broadcast to {} users", activeSessions.size());
        } catch (Exception e) {
            log.error("❌ Failed to broadcast message: {}", e.getMessage());
        }
    }

    /**
     * Mark user as online in Redis
     */
    private Mono<Long> markUserOnline(String userId) {
        return redisTemplate.opsForSet()
                .add(ONLINE_USERS_KEY, userId)
                .doOnSuccess(added -> log.info("✅ User {} marked as online", userId));
    }

    /**
     * Mark user as offline in Redis
     */
    private Mono<Long> markUserOffline(String userId) {
        return redisTemplate.opsForSet()
                .remove(ONLINE_USERS_KEY, userId)
                .doOnSuccess(removed -> log.info("✅ User {} marked as offline", userId));
    }

    /**
     * Get count of online users
     */
    public Mono<Long> getOnlineUsersCount() {
        return redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
    }

    /**
     * Check if user is online
     */
    public Mono<Boolean> isUserOnline(String userId) {
        return redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
    }
}