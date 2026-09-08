package com.yunshu.mes.production.vo;

public record ProductionCompletionVO(
        Long completionId,
        String completionNo,
        String workOrderNo,
        String completedQty,
        String defectQty,
        String completionTime,
        String status
) {
}
