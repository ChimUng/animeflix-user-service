package com.animeflix.animecatalogservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleNotFound(NotFoundException ex) {
        ApiResponse<?> response = new ApiResponse<>(
                false, ex.getVietnameseMessage(), null, java.time.LocalDateTime.now()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
    }

    @ExceptionHandler(BadRequestException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleBadRequest(BadRequestException ex) {
        ApiResponse<?> response = new ApiResponse<>(
                false, ex.getVietnameseMessage(), null, java.time.LocalDateTime.now()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleBusinessException(BusinessException ex) {
        ApiResponse<?> response = new ApiResponse<>(
                false, ex.getVietnameseMessage(), null, java.time.LocalDateTime.now()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleAll(Exception ex) {
        ApiResponse<?> response = ApiResponse.error("Hệ thống đang bận, vui lòng thử lại sau!");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}