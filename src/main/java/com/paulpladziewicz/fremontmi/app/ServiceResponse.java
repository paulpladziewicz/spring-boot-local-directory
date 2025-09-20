package com.paulpladziewicz.fremontmi.app;

public record ServiceResponse<T>(T value, String errorCode) {

    public static <T> ServiceResponse<T> value(T value) {
        return new ServiceResponse<>(value, null);
    }

    public static <T> ServiceResponse<T> error(String errorCode) {
        return new ServiceResponse<>(null, errorCode);
    }

    public boolean hasError() {
        return errorCode != null;
    }
}
