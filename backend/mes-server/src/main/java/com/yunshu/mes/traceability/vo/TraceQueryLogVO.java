package com.yunshu.mes.traceability.vo;

public record TraceQueryLogVO(
        Long traceQueryId,
        String queryType,
        String queryKey,
        String queryTime
) {
}
