package com.animeflix.userservice.repository;

import com.animeflix.userservice.entity.UserPreference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserPreferenceRepository extends ReactiveMongoRepository<UserPreference, String> {

    // Tìm preference của user
    Mono<UserPreference> findByUserId(String userId);

    // Xóa preference
    Mono<Void> deleteByUserId(String userId);
}
