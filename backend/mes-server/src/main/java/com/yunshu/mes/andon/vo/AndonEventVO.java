package com.yunshu.mes.andon.vo;

public record AndonEventVO(
        Long andonId,
        String andonNo,
        String typeName,
        String lineName,
        String exceptionDesc,
        String status,
        String occurTime
) {
}
