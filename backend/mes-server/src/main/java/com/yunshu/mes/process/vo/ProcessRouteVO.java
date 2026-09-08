package com.yunshu.mes.process.vo;

public record ProcessRouteVO(
        Long routeId,
        String routeCode,
        String routeName,
        String routeVersion,
        String status
) {
}
