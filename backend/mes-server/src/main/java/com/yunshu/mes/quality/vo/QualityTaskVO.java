package com.yunshu.mes.quality.vo;

public record QualityTaskVO(
        Long qualityTaskId,
        String qualityTaskNo,
        String productName,
        String inspectType,
        String status
) {
}
