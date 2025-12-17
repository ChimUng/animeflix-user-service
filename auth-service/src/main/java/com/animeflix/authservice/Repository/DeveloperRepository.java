package com.animeflix.authservice.Repository;

import com.animeflix.authservice.Entity.Developer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DeveloperRepository extends ReactiveMongoRepository<Developer, String> {
    Mono<Developer> findByApiKey(String apiKey);
    Mono<Developer> findByAppId(String appId);
    Mono<Developer> findByEmail(String email);
    Mono<Developer> findByUsername(String username);
}