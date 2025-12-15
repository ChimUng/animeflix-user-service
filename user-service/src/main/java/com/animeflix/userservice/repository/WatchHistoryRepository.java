package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.WatchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WatchHistoryRepository extends ReactiveMongoRepository<WatchHistory, String> {


    Flux<WatchHistory> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Flux<WatchHistory> findByUserIdAndAniIdOrderByCreatedAtDesc(String userId, String aniId);

    Mono<WatchHistory> findByUserIdAndAniIdAndEpId(String userId, String aniId, String epId);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $group: { _id: '$aniId' } }",
            "{ $count: 'total' }"
    })
    Mono<Long> countDistinctAnimeByUserId(String userId);

    Flux<WatchHistory> findTop20ByUserIdOrderByCreatedAtDesc(String userId);

    Flux<WatchHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    Mono<Void> deleteByUserIdAndAniId(String userId, String aniId);

    Mono<Void> deleteByUserId(String userId);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $group: { _id: null, total: { $sum: '$timeWatched' } } }"
    })
    Mono<Long> getTotalWatchedSeconds(String userId);

    Flux<WatchHistory> findByUserIdAndProviderOrderByCreatedAtDesc(String userId, String provider);

    Flux<WatchHistory> findByUserIdAndSubtypeOrderByCreatedAtDesc(String userId, String subtype);

    Flux<WatchHistory> findByUserIdAndCompletedFalseOrderByCreatedAtDesc(String userId);
}