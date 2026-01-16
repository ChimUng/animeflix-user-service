package com.animeflix.animetranslate.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TranslationException.class)
    public ResponseEntity<String> handleTranslationException(TranslationException e) {
        logger.error("Translation error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    // Thêm handlers cho subclasses nếu cần response khác
    @ExceptionHandler(ParseTranslationException.class)
    public ResponseEntity<String> handleParseException(ParseTranslationException e) {
        logger.warn("Parse error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid translation format: " + e.getMessage());
    }
}