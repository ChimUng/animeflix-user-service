package com.animeflix.animetranslate.exception;

public class ValidationException extends TranslationException {
    public ValidationException(String message, Throwable e) {
        super(message);
    }
}