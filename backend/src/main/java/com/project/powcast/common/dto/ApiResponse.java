package com.project.powcast.common.dto;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String code,
        String message,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, null, message, OffsetDateTime.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, code, message, OffsetDateTime.now());
    }
}

