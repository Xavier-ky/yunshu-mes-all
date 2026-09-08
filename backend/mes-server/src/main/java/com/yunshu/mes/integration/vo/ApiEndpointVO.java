package com.yunshu.mes.integration.vo;

public record ApiEndpointVO(
        Long endpointId,
        String endpointCode,
        String endpointName,
        String apiPath,
        String httpMethod,
        String direction,
        String status
) {
}
