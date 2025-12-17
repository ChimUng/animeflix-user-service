package com.animeflix.animecatalogservice.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final String vietnameseMessage;

    public BusinessException(String code, String message, String vietnameseMessage) {
        super(message);
        this.code = code;
        this.vietnameseMessage = vietnameseMessage;
    }

    public BusinessException(String message) {
        super(message);
        this.code = "UNKNOWN_ERROR";
        this.vietnameseMessage = "Đã có lỗi xảy ra";
    }
}
