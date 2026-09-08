package com.yunshu.mes.process.vo;

public record ProcessStepVO(
        Long stepId,
        String stepCode,
        String stepName,
        String stepType,
        String standardHours,
        String status
) {
}
