package com.animeflix.userservice.service;

import com.animeflix.userservice.dto.response.NotificationResponse;
import com.animeflix.userservice.entity.Notification;
import com.animeflix.userservice.exception.ResourceNotFoundException;
import com.animeflix.userservice.mapper.NotificationMapper;
import com.animeflix.userservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final NotificationMapper mapper;

    /**
     * Tạo notification mới
     */
    public Mono<Notification> createNotification(
            String userId,
            Notification.NotificationType type,
            String title,
            String message,
            String animeId,
            Integer episodeNumber,
            String imageUrl) {

        // Check duplicate trong 1 giờ gần đây (tránh spam)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        return notificationRepo.existsByUserIdAndAnimeIdAndEpisodeNumberAndCreatedAtAfter(
                        userId, animeId, episodeNumber, oneHourAgo)
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("Skipping duplicate notification for user: {}, anime: {}, ep: {}",
                                userId, animeId, episodeNumber);
                        return Mono.empty();
                    }

                    LocalDateTime now = LocalDateTime.now();
                    Date expiresAt = Date.from(
                            now.plusDays(30).atZone(ZoneId.systemDefault()).toInstant()
                    );

                    Notification notification = Notification.builder()
                            .userId(userId)
                            .type(type)
                            .title(title)
                            .message(message)
                            .imageUrl(imageUrl)
                            .animeId(animeId)
                            .episodeNumber(episodeNumber)
                            .actionUrl(String.format("/anime/%s/episode/%d", animeId, episodeNumber))
                            .isRead(false)
                            .createdAt(now)
                            .expiresAt(expiresAt)
                            .build();

                    return notificationRepo.save(notification);
                });
    }

    /**
     * Lấy danh sách thông báo (phân trang)
     */
    public Flux<NotificationResponse> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    /**
     * Lấy thông báo chưa đọc
     */
    public Flux<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepo.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .map(mapper::toResponse);
    }

    /**
     * Đếm thông báo chưa đọc
     */
    public Mono<Long> countUnread(String userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu đã đọc
     */
    public Mono<NotificationResponse> markAsRead(String userId, String notificationId) {
        return notificationRepo.findById(notificationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Notification not found")))
                .flatMap(notification -> {
                    if (!notification.getUserId().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Notification not found"));
                    }

                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    return notificationRepo.save(notification);
                })
                .map(mapper::toResponse);
    }

    /**
     * Đánh dấu tất cả là đã đọc
     */
    public Mono<Long> markAllAsRead(String userId) {
        return notificationRepo.findByUserIdAndIsReadFalse(userId)
                .flatMap(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    return notificationRepo.save(notification);
                })
                .count();
    }

    /**
     * Xóa thông báo
     */
    public Mono<Void> deleteNotification(String userId, String notificationId) {
        return notificationRepo.findById(notificationId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Notification not found")))
                .flatMap(notification -> {
                    if (!notification.getUserId().equals(userId)) {
                        return Mono.error(new ResourceNotFoundException("Notification not found"));
                    }
                    return notificationRepo.delete(notification);
                });
    }

    /**
     * Xóa tất cả thông báo
     */
    public Mono<Void> deleteAllNotifications(String userId) {
        return notificationRepo.deleteByUserId(userId);
    }

    /**
     * Cleanup thông báo hết hạn (cho scheduler)
     */
    public Mono<Void> cleanupExpiredNotifications() {
        Date now = new Date();
        return notificationRepo.deleteByExpiresAtBefore(now)
                .doOnSuccess(v -> log.info("Cleaned up expired notifications"));
    }
}