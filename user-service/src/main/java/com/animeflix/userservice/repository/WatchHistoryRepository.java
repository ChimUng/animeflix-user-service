package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.WatchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WatchHistoryRepository extends ReactiveMongoRepository<WatchHistory, String> {

    // ========== QUERIES SỬ DỤNG FIELD MỚI (aniId, epId, createdAt) ==========

    // Lấy lịch sử xem của user (phân trang) - SẮP XẾP THEO createdAt
    Flux<WatchHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Tìm lịch sử xem cụ thể của anime - DÙNG aniId
    Flux<WatchHistory> findByUserIdAndAniIdOrderByCreatedAtDesc(String userId, String aniId);

    // Tìm lịch sử xem episode cụ thể - DÙNG epId
    Mono<WatchHistory> findByUserIdAndAniIdAndEpId(String userId, String aniId, String epId);

    // Đếm số anime đã xem (distinct) - DÙNG aniId
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $group: { _id: '$aniId' } }",
            "{ $count: 'total' }"
    })
    Mono<Long> countDistinctAnimeByUserId(String userId);

    // Lấy anime đã xem gần đây (cho recommendations)
    Flux<WatchHistory> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

    // Xóa lịch sử của anime cụ thể - DÙNG aniId
    Mono<Void> deleteByUserIdAndAniId(String userId, String aniId);

    // Xóa toàn bộ lịch sử của user
    Mono<Void> deleteByUserId(String userId);

    // Tổng thời gian xem - DÙNG timeWatched
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $group: { _id: null, total: { $sum: '$timeWatched' } } }"
    })
    Mono<Long> getTotalWatchedSeconds(String userId);

    // ========== QUERIES BỔ SUNG CHO SCHEMA MỚI ==========

    // Lấy anime theo provider (gogoanime, zoro, etc.)
    Flux<WatchHistory> findByUserIdAndProviderOrderByCreatedAtDesc(String userId, String provider);

    // Lấy anime theo subtype (sub/dub)
    Flux<WatchHistory> findByUserIdAndSubtypeOrderByCreatedAtDesc(String userId, String subtype);

    // Lấy anime chưa xem xong (completed = false hoặc null)
    Flux<WatchHistory> findByUserIdAndCompletedFalseOrderByCreatedAtDesc(String userId);
}