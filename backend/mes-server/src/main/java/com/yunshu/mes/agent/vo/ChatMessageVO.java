package com.yunshu.mes.agent.vo;

public record ChatMessageVO(
        String role,
        String content,
        String time
) {
}
