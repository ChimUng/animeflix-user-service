package com.animeflix.animeepisode.exception;

public class EpisodeFetchException extends RuntimeException {
    public EpisodeFetchException(String message) { super(message); }
    public EpisodeFetchException(String message, Throwable cause) { super(message, cause); }
}
