package com.law4x.common.interfaces.rest;

public enum ApiErrorCode {
    SUCCESS(200, "success"),
    FAILURE(-1, "failure");

    private final int code;
    private final String defaultMessage;

    ApiErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
