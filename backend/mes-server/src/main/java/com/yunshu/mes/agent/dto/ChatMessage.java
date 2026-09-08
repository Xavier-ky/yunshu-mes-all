package com.yunshu.mes.agent.dto;

/**
 * LLM 消息（role + content），用于拼装大模型对话上下文。
 */
public record ChatMessage(
        String role,
        String content
) {
}
