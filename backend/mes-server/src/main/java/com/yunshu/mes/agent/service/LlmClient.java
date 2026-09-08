package com.yunshu.mes.agent.service;

import com.yunshu.mes.agent.dto.ChatMessage;
import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型客户端接口。真实实现 {@link com.yunshu.mes.agent.service.impl.OpenAiCompatibleLlmClient}
 * 通过 OpenAI 兼容协议流式调用（DeepSeek / DashScope / 智谱 GLM 均兼容）。
 *
 * providerKey 对应 mes.agent.llm.providers 中的键（deepseek/qwen/glm），由前端模型选择器下发。
 */
public interface LlmClient {

    void streamChat(List<ChatMessage> messages, String providerKey, Consumer<String> tokenConsumer) throws Exception;
}
