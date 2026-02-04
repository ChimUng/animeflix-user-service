package com.animeflix.animeepisode.repository;

import com.animeflix.animeepisode.exception.CacheException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@Slf4j
public class RedisEpisodeRepository {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEpisodeRepository(ReactiveRedisTemplate<String, String> reactiveRedisTemplate, ObjectMapper objectMapper) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * ✅ FIX: Get cached data - Return empty if key not found, don't throw exception
     *
     * @param key Cache key
     * @return Cached JSON string or empty Mono if not found
     */
    public Mono<String> getCachedData(String key) {
        return reactiveRedisTemplate.opsForValue().get(key)
                .filter(cached -> cached != null && !cached.isEmpty())
                .doOnNext(cached -> log.debug("✅ Cache hit: {}", key))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("⚠️ Cache miss: {}", key);
                    return Mono.empty();
                }))
                .onErrorResume(e -> {
                    // ✅ FIX: Don't throw exception, just log and return empty
                    // Key không tồn tại là case bình thường, không phải lỗi!
                    log.warn("⚠️ Redis get error for key {}: {} - Treating as cache miss",
                            key, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * ✅ Set cached data with TTL
     *
     * @param key Cache key
     * @param data Data to cache
     * @param cacheTimeSeconds TTL in seconds
     * @return Void Mono
     */
    public <T> Mono<Void> setCachedData(String key, T data, long cacheTimeSeconds) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return reactiveRedisTemplate.opsForValue()
                    .set(key, json, Duration.ofSeconds(cacheTimeSeconds))
                    .doOnSuccess(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            log.info("✅ Cached: {} (TTL: {}s)", key, cacheTimeSeconds);
                        } else {
                            log.warn("⚠️ Cache set failed: {}", key);
                        }
                    })
                    .then()
                    .onErrorResume(e -> {
                        // ✅ FIX: Don't crash on cache write failure - just log
                        log.error("❌ Redis set error for key {}: {}", key, e.getMessage());
                        return Mono.empty();  // Continue without caching
                    });
        } catch (JsonProcessingException e) {
            log.error("❌ JSON serialization error for key {}: {}", key, e.getMessage());
            return Mono.empty();  // Continue without caching
        }
    }

    /**
     * ✅ Delete cache key
     *
     * @param key Cache key to delete
     * @return Void Mono
     */
    public Mono<Void> deleteKey(String key) {
        return reactiveRedisTemplate.delete(key)
                .doOnSuccess(count -> {
                    if (count > 0) {
                        log.info("🗑️ Deleted cache: {}", key);
                    }
                })
                .then()
                .onErrorResume(e -> {
                    log.warn("⚠️ Redis delete error for key {}: {}", key, e.getMessage());
                    return Mono.empty();
                });
    }
}