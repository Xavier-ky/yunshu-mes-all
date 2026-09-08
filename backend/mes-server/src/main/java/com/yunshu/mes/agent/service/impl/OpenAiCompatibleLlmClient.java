package com.yunshu.mes.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.mes.agent.config.AgentLlmProperties;
import com.yunshu.mes.agent.config.AgentLlmProperties.Provider;
import com.yunshu.mes.agent.dto.ChatMessage;
import com.yunshu.mes.agent.service.LlmClient;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容协议的流式大模型客户端。用 JDK 内置 HttpClient（无需额外依赖）。
 * 支持 DeepSeek / 阿里 DashScope 兼容 / 智谱 GLM 等任何 OpenAI 兼容端点。
 *
 * 流程：POST {baseUrl}/chat/completions（stream=true, Bearer apiKey）
 *      → 逐行读 SSE（data: {json}），提取 choices[0].delta.content，喂给 tokenConsumer。
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final AgentLlmProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public OpenAiCompatibleLlmClient(AgentLlmProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public void streamChat(List<ChatMessage> messages, String providerKey, Consumer<String> tokenConsumer) throws Exception {
        Provider provider = resolveProvider(providerKey);

        List<Map<String, String>> msgList = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, String> row = new HashMap<>();
            row.put("role", m.role());
            row.put("content", m.content());
            msgList.add(row);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", provider.getModel());
        body.put("messages", msgList);
        body.put("stream", true);
        String bodyJson = objectMapper.writeValueAsString(body);

        String url = provider.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + provider.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            String errText;
            try (InputStream bs = resp.body()) {
                errText = new String(bs.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new RuntimeException("LLM 返回 " + resp.statusCode() + ": " + errText);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                    Object choicesObj = chunk.get("choices");
                    if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                        continue;
                    }
                    if (!(choices.get(0) instanceof Map<?, ?> choice)) {
                        continue;
                    }
                    if (!(choice.get("delta") instanceof Map<?, ?> delta)) {
                        continue;
                    }
                    Object content = delta.get("content");
                    if (content instanceof String s && !s.isEmpty()) {
                        tokenConsumer.accept(s);
                    }
                } catch (Exception ignored) {
                    // 单行解析失败不影响整体流
                }
            }
        }
    }

    private Provider resolveProvider(String providerKey) {
        Map<String, Provider> providers = props.getProviders();
        String key = (providerKey == null || providerKey.isBlank()) ? props.getDefaultProvider() : providerKey;
        Provider p = providers.get(key);
        if (p == null) {
            p = providers.get(props.getDefaultProvider());
        }
        if (p == null || p.getApiKey() == null || p.getApiKey().isBlank()) {
            throw new RuntimeException("未配置可用的 LLM provider（" + key
                    + "），请在 application-local.yml 设置 mes.agent.llm.providers.*.api-key");
        }
        return p;
    }
}
