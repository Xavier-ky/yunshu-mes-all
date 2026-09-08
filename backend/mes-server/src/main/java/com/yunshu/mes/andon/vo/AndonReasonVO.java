package com.yunshu.mes.andon.vo;

public record AndonReasonVO(
        Long reasonId,
        String reasonCode,
        String reasonName,
        String typeName,
        String status
) {
}
