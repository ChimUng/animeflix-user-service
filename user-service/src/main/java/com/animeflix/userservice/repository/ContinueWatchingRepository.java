package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.ContinueWatching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ContinueWatchingRepository extends ReactiveMongoRepository<ContinueWatching, String> {

    // Lấy danh sách "Xem tiếp" (max 20 items)
    Flux<ContinueWatching> findByUserIdOrderByLastWatchedAtDesc(String userId, Pageable pageable);

    // Tìm anime cụ thể - DÙNG aniId
    Mono<ContinueWatching> findByUserIdAndAniId(String userId, String aniId);

    // Xóa anime khỏi continue watching - DÙNG aniId
    Mono<Void> deleteByUserIdAndAniId(String userId, String aniId);

    // Đếm số anime trong continue watching
    Mono<Long> countByUserId(String userId);

    // Lấy danh sách cũ nhất (để cleanup khi vượt quá limit)
    Flux<ContinueWatching> findByUserIdOrderByLastWatchedAtAsc(String userId);
}