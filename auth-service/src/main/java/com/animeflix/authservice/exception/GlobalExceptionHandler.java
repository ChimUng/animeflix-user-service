package com.animeflix.authservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Map<String, String>>> handleRuntimeException(RuntimeException ex) {
        if (!(ex instanceof UnsupportedOperationException) &&
                !ex.getClass().getPackageName().contains("org.springframework") &&
                !ex.getClass().getPackageName().contains("com.mongodb")) {
            log.error("Business Error: {}", ex.toString(), ex);
        }

        String message = ex.getMessage();
        if (message == null || message.isBlank()) message = "Bad request";

        return Mono.just(ResponseEntity.badRequest().body(Map.of("error", message)));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, String>>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        Map<String, String> error = Map.of("error", "Internal server error");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}