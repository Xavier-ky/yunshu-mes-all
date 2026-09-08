package com.yunshu.mes.quality.vo;

public record ReworkVO(
        Long reworkId,
        String reworkNo,
        String defectNo,
        String reworkReason,
        String status
) {
}
