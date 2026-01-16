package com.animeflix.animestream.exception;

public class StreamFetchException extends RuntimeException {
    public StreamFetchException(String message) {
        super(message);
    }

    public StreamFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}