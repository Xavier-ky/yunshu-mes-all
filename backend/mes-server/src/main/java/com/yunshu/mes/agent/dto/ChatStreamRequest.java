package com.yunshu.mes.agent.dto;

public record ChatStreamRequest(
        String prompt,
        String session_id,
        String model
) {
}
