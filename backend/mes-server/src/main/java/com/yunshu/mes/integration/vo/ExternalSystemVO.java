package com.yunshu.mes.integration.vo;

public record ExternalSystemVO(
        Long systemId,
        String systemCode,
        String systemName,
        String systemType,
        String status
) {
}
