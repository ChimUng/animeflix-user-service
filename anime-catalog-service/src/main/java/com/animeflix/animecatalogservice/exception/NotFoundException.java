package com.animeflix.animecatalogservice.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message, "Không tìm thấy dữ liệu");
    }
}
