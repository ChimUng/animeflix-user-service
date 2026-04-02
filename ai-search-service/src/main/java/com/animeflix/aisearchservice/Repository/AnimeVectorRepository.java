package com.animeflix.aisearchservice.Repository;

import com.animeflix.aisearchservice.Entity.AnimeVector;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AnimeVectorRepository extends ReactiveMongoRepository<AnimeVector, String> {

    Flux<AnimeVector> findByEmbeddedFalseOrEmbeddedIsNull(Pageable pageable);

    Mono<Long> countByEmbeddedFalseOrEmbeddedIsNull();

    Mono<Long> countByEmbeddedTrue();

    Flux<AnimeVector> findByGenresContainingOrderByPopularityDesc(String genre, Pageable pageable);
}