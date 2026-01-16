package com.animeflix.animestream.Repository;

import com.animeflix.animestream.exception.CacheException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Repository for Redis caching operations.
 */
@Repository
public class RedisVideoRepository {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisVideoRepository(ReactiveRedisTemplate<String, String> reactiveRedisTemplate, ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get cached data from Redis.
     * @param key Cache key.
     * @return Mono emitting cached JSON string or empty.
     */
    public Mono<String> getCachedData(String key) {
        return reactiveRedisTemplate.opsForValue().get(key)
                .filter(cached -> !cached.isEmpty())
                .switchIfEmpty(Mono.defer(() -> deleteKey(key).then(Mono.empty())))
                .onErrorMap(e -> new CacheException("Error getting cache for key: " + key, e));
    }

    /**
     * Set cached data in Redis with expiration.
     * @param key Cache key.
     * @param data Data to cache.
     * @param cacheTimeSeconds Cache expiration time in seconds.
     * @return Mono emitting completion.
     */
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

    /**
     * Delete cache key.
     * @param key Cache key.
     * @return Mono emitting completion.
     */
    public Mono<Void> deleteKey(String key) {
        return reactiveRedisTemplate.delete(key).then();
    }
}