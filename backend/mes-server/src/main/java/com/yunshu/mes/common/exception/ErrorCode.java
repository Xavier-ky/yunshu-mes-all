package com.yunshu.mes.common.exception;

public enum ErrorCode {
    BAD_REQUEST("BAD_REQUEST", "请求参数不合法"),
    UNAUTHORIZED("UNAUTHORIZED", "账号或密码不正确"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误"),
    CRYPTO_ERROR("CRYPTO_ERROR", "加密或解密失败");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
