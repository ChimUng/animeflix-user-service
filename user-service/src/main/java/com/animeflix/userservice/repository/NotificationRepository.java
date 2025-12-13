package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Date;

public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    // Lấy thông báo của user (phân trang)
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Lấy thông báo chưa đọc
    Flux<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    // Đếm thông báo chưa đọc
    Mono<Long> countByUserIdAndIsReadFalse(String userId);

    // Lấy thông báo theo type
    Flux<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId,
            Notification.NotificationType type,
            Pageable pageable
    );

    // Đánh dấu tất cả là đã đọc
    Flux<Notification> findByUserIdAndIsReadFalse(String userId);

    // Xóa thông báo cũ (cho scheduler cleanup)
    Mono<Void> deleteByExpiresAtBefore(Date date);

    // Xóa tất cả thông báo của user
    Mono<Void> deleteByUserId(String userId);

    // Kiểm tra notification trùng (tránh spam)
    Mono<Boolean> existsByUserIdAndAnimeIdAndEpisodeNumberAndCreatedAtAfter(
            String userId,
            String animeId,
            Integer episodeNumber,
            LocalDateTime createdAt
    );
}