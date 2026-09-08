package com.yunshu.mes.factory.vo;

public record StationStatusRatioVO(
        int running,
        int warning,
        int fault,
        int changeover,
        int idle
) {
}
