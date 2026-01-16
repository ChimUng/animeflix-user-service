package com.animeflix.animeepisode.repository;

import com.animeflix.animeepisode.exception.CacheException;
import com.animeflix.animeepisode.model.Provider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisEpisodeRepository {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEpisodeRepository(ReactiveRedisTemplate<String, String> reactiveRedisTemplate, ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<String> getCachedData(String key) {
        return reactiveRedisTemplate.opsForValue().get(key)
                .filter(cached -> !cached.isEmpty())
                .switchIfEmpty(Mono.defer(() -> deleteKey(key).then(Mono.empty())))
                .onErrorMap(e -> new CacheException("Error getting cache for key: " + key, e));
    }

    public <T> Mono<Void> setCachedData(String key, T data, long cacheTimeSeconds) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return reactiveRedisTemplate.opsForValue()
                    .set(key, json, Duration.ofSeconds(cacheTimeSeconds))
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(new CacheException("Error serializing data for key: " + key, e));
        }
    }

    public Mono<Void> deleteKey(String key) {
        return reactiveRedisTemplate.delete(key).then();
    }
}