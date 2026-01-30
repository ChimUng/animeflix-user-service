package com.animeflix.animeepisode.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EpisodeFetchException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleEpisodeFetchException(
            EpisodeFetchException ex) {
        log.error("❌ Episode fetch error: {}", ex.getMessage(), ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to fetch episodes: " + ex.getMessage()))
        );
    }

    @ExceptionHandler(CacheException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleCacheException(
            CacheException ex) {
        log.warn("⚠️ Cache error: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Cache operation failed"))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("⚠️ Invalid argument: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(ex.getMessage()))
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        log.error("❌ Unexpected error: {}", ex.getMessage(), ex);
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("An unexpected error occurred"))
        );
    }
}