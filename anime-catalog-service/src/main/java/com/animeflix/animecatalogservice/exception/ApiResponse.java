package com.animeflix.animecatalogservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // Success
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    // Error
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, LocalDateTime.now());
    }
}