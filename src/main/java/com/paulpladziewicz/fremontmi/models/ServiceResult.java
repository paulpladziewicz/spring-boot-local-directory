package com.paulpladziewicz.fremontmi.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceResult<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private LocalDateTime timestamp;

    public ServiceResult(boolean success, String message, T data, String errorCode, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, null, data, null, LocalDateTime.now());
    }

    public static <T> ServiceResult<T> success() {
        return new ServiceResult<>(true, null, null, null, LocalDateTime.now());
    }

    public static <T> ServiceResult<T> error(String message, String errorCode) {
        return new ServiceResult<>(false, message, null, errorCode, LocalDateTime.now());
    }
}
