package com.yunshu.mes.andon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 安灯原因创建/更新请求。
 */
public record AndonReasonRequest(
        @NotBlank @Size(max = 64) String reasonCode,
        @NotBlank @Size(max = 100) String reasonName,
        @Size(max = 64) String reasonCategory,
        @Size(max = 32) String status
) {
    public AndonReasonRequest {
        if (status == null || status.isBlank()) {
            status = "ENABLED";
        }
        if (reasonCategory == null || reasonCategory.isBlank()) {
            reasonCategory = "GENERAL";
        }
    }
}
