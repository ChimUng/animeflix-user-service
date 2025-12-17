package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.kafka.NewEpisodeEvent;
import com.animeflix.userservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpisodeEventConsumer {

    private final FavoriteService favoriteService;
    private final NotificationService notificationService;
    private final UserPreferenceService preferenceService;
    private final WebSocketNotificationService webSocketService;

    /**
     * Kafka listener - Process new episode events
     *
     * Consumer group: user-service-notifications
     * Topic: anime.episode.new
     * Concurrency: 3 (configured in KafkaConsumerConfig)
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.new-episode}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleNewEpisode(
            @Payload NewEpisodeEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("📨 Received new episode event: anime={}, episode={}, partition={}, offset={}",
                event.getAnimeId(),
                event.getEpisodeNumber(),
                partition,
                offset);

        try {
            processEvent(event)
                    .doOnSuccess(count -> {
                        log.info("✅ Processed event successfully: {} notifications created", count);
                        acknowledgment.acknowledge(); // Manual commit
                    })
                    .doOnError(error -> {
                        log.error("❌ Error processing event: {}", error.getMessage(), error);
                        // Don't acknowledge - message will be reprocessed
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("❌ Fatal error processing event: {}", e.getMessage(), e);
            // Acknowledge to prevent infinite retry
            acknowledgment.acknowledge();
        }
    }

    /**
     * Process event: Create notifications and push to online users
     */
    private reactor.core.publisher.Mono<Integer> processEvent(NewEpisodeEvent event) {
        log.debug("🔍 Finding users following anime: {}", event.getAnimeId());

        // Get users with this anime in favorites
        return favoriteService.getFavoritesWithNotification()
                .filter(fav -> fav.getAnimeId().equals(event.getAnimeId()))
                .flatMap(favorite -> {
                    // Check user preferences
                    return preferenceService.isNotificationEnabled(favorite.getUserId())
                            .flatMap(enabled -> {
                                if (!enabled) {
                                    log.debug("⏭️ Notifications disabled for user: {}",
                                            favorite.getUserId());
                                    return reactor.core.publisher.Mono.empty();
                                }

                                // Create notification
                                return createNotification(event, favorite.getUserId())
                                        .flatMap(notification -> {
                                            // Push to WebSocket if user online
                                            webSocketService.sendToUser(
                                                    favorite.getUserId(),
                                                    notification
                                            );
                                            return reactor.core.publisher.Mono.just(1);
                                        });
                            });
                })
                .reduce(0, Integer::sum)
                .doOnNext(count -> log.info("👥 Notified {} users", count));
    }

    /**
     * Create notification in MongoDB
     */
    private reactor.core.publisher.Mono<com.animeflix.userservice.entity.Notification>
    createNotification(NewEpisodeEvent event, String userId) {

        String title = "New Episode Available!";
        String message = String.format(
                "%s - Episode %d is now available",
                event.getAnimeTitle(),
                event.getEpisodeNumber()
        );

        return notificationService.createNotification(
                userId,
                Notification.NotificationType.NEW_EPISODE,
                title,
                message,
                event.getAnimeId(),
                event.getEpisodeNumber(),
                event.getCoverImage()
        );
    }
}