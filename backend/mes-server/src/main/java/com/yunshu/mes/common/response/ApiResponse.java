package com.yunshu.mes.common.response;

import jakarta.servlet.http.HttpServletRequest;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId
) {
    public static final String TRACE_ID_ATTRIBUTE = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>("SUCCESS", "ok", data, traceId);
    }

    public static <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return success(data, traceId(request));
    }

    public static <T> ApiResponse<T> fail(String code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }

    public static <T> ApiResponse<T> fail(String code, String message, HttpServletRequest request) {
        return fail(code, message, traceId(request));
    }

    public static String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
        return traceId == null ? "" : String.valueOf(traceId);
    }
}
