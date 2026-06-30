package com.law4x.common.interfaces.rest;

public record ApiResponse<T>(
        int code,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiErrorCode.SUCCESS.code(), ApiErrorCode.SUCCESS.defaultMessage(), data);
    }

    public static <T> ApiResponse<T> error(ApiErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
