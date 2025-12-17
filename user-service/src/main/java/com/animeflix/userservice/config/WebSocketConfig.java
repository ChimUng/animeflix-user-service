package com.animeflix.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration using STOMP over WebSocket
 *
 * Endpoints:
 * - /ws - WebSocket connection endpoint
 * - /app - Application destination prefix
 * - /topic - Broadcast to all users
 * - /user - User-specific messages
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register WebSocket endpoint
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")  // WebSocket endpoint
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://animeflix.vercel.app",
                        "https://*.animeflix.com"
                )
                .withSockJS();  // Fallback for browsers without WebSocket support

        log.info("✅ WebSocket endpoint registered: /ws");
    }

    /**
     * Configure message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for /topic and /user
        registry.enableSimpleBroker("/topic", "/user");

        // Set application destination prefix
        registry.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix
        registry.setUserDestinationPrefix("/user");

        log.info("✅ Message broker configured");
    }
}