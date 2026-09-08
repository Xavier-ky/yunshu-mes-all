package com.yunshu.mes.factory.vo;

public record EventLogVO(
        String eventType,
        String message,
        String eventTime
) {
}
