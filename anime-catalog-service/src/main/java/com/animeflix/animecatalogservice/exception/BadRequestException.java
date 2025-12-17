package com.animeflix.animecatalogservice.exception;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", message, "Yêu cầu không hợp lệ");
    }
}
