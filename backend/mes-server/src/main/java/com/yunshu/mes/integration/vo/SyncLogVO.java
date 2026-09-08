package com.yunshu.mes.integration.vo;

public record SyncLogVO(
        Long syncLogId,
        String syncType,
        String bizNo,
        String syncStatus,
        String syncTime
) {
}
