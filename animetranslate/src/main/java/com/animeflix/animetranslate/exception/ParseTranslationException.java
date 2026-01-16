package com.animeflix.animetranslate.exception;

public class ParseTranslationException extends TranslationException {
    public ParseTranslationException(String message) {
        super(message);
    }

    public ParseTranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}