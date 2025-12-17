package com.animeflix.authservice.Repository;

import com.animeflix.authservice.Entity.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);
    Mono<User> findByUsername(String username);
}