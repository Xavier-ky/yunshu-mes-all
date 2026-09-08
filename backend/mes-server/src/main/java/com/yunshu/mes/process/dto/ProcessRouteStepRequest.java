package com.yunshu.mes.process.dto;

import jakarta.validation.constraints.NotNull;

public record ProcessRouteStepRequest(
        @NotNull Long routeId,
        @NotNull Long stepId,
        @NotNull Integer stepSeq,
        String stationType,
        Boolean isMustPass
) {
    public ProcessRouteStepRequest {
        if (stationType == null) stationType = "";
        if (isMustPass == null) isMustPass = true;
    }
}
