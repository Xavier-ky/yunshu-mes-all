package com.yunshu.mes.factory.vo;

public record ShiftVO(
        Long shiftId,
        String shiftCode,
        String shiftName,
        String startTime,
        String endTime,
        String status
) {
}
