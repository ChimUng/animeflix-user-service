package com.animeflix.authservice.Repository;

import com.animeflix.authservice.Entity.Session;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SessionRepository extends ReactiveMongoRepository<Session, String> {
    Mono<Session> findByRefreshToken(String refreshToken);
    Flux<Session> findByUserIdAndIsRevokedFalse(String userId);
    Mono<Void> deleteByUserId(String userId);
}
