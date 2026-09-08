package com.yunshu.mes.quality.vo;

public record DefectVO(
        Long defectId,
        String defectNo,
        String snCode,
        String workOrderNo,
        String defectDesc,
        String severity,
        String status
) {
}
