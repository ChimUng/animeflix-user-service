package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.Favorite;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FavoriteRepository extends ReactiveMongoRepository<Favorite, String> {

    // Lấy danh sách yêu thích (phân trang)
    Flux<Favorite> findByUserIdOrderByAddedAtDesc(String userId, Pageable pageable);

    // Lấy tất cả yêu thích (không phân trang)
    Flux<Favorite> findByUserIdOrderByAddedAtDesc(String userId);

    // Kiểm tra đã yêu thích chưa
    Mono<Boolean> existsByUserIdAndAnimeId(String userId, String animeId);

    // Tìm favorite cụ thể
    Mono<Favorite> findByUserIdAndAnimeId(String userId, String animeId);

    // Xóa khỏi yêu thích
    Mono<Void> deleteByUserIdAndAnimeId(String userId, String animeId);

    // Đếm số anime yêu thích
    Mono<Long> countByUserId(String userId);

    // Lấy danh sách anime cần thông báo (cho scheduler)
    Flux<Favorite> findByNotifyNewEpisodeTrue();

    // Lấy favorites có bật thông báo của user
    Flux<Favorite> findByUserIdAndNotifyNewEpisodeTrue(String userId);
}
