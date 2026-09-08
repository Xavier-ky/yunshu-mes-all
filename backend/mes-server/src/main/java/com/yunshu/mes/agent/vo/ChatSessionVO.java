package com.yunshu.mes.agent.vo;

/**
 * AI 对话会话，字段名与前端 AICommandCenter 约定一致（snake_case）。
 */
public record ChatSessionVO(
        String session_id,
        String title,
        String session_summary,
        String created_at,
        String last_message_at,
        String latest_message
) {
}
