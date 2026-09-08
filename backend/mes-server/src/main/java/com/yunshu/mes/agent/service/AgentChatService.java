package com.yunshu.mes.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.mes.agent.config.AgentLlmProperties;
import com.yunshu.mes.agent.dto.ChatMessage;
import com.yunshu.mes.agent.repository.AiChatRepository;
import com.yunshu.mes.agent.repository.AiChatRepository.SessionRow;
import com.yunshu.mes.agent.vo.ChatMessageVO;
import com.yunshu.mes.agent.vo.ChatSessionVO;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 聊天服务：会话 CRUD + 流式对话编排（SSE）。
 *
 * 流程：获取/新建会话 → 载入历史 → 落库 user 消息 → 发 session 事件 →
 *      调 LlmClient 流式生成（逐 token 发 token 事件）→ 落库 assistant 消息 → 发 done 事件。
 *
 * 与大模型的耦合仅限于 {@link LlmClient} 接口；与其他业务模块的耦合仅限于
 * {@link AgentToolRegistry}（Agent 下发 tool 指令时调用），本服务不直接依赖任何业务模块。
 */
@Service
public class AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);
    private static final String SYSTEM_PROMPT =
            "你是云枢智造 MES 的智能助手，面向电风扇制造的车间管理、质量、设备、追溯等场景。"
                    + "回答结论先行、用制造工程师语言、给出可执行步骤。"
                    + "如需打开某页面，可在回复末尾追加一行 JSON 指令："
                    + "{\"type\":\"navigate\",\"path\":\"/system/users\"}；"
                    + "如需调用业务工具，追加：{\"type\":\"tool\",\"name\":\"工具名\",\"args\":{}}。"
                    + "若用户询问你的底层模型，请如实说明当前接入的模型（见系统上下文）。";

    private final AiChatRepository repository;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final AgentLlmProperties llmProperties;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentChatService(AiChatRepository repository, LlmClient llmClient, ObjectMapper objectMapper,
                            AgentLlmProperties llmProperties) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.llmProperties = llmProperties;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    public List<ChatSessionVO> listSessions(int limit) {
        return repository.listSessions(limit);
    }

    public List<ChatMessageVO> listMessages(String sessionUuid, int limit) {
        SessionRow session = repository.findSessionByUuid(sessionUuid);
        if (session == null) {
            return List.of();
        }
        return repository.listMessages(session.id(), limit);
    }

    public boolean deleteSession(String sessionUuid) {
        return repository.deleteSession(sessionUuid);
    }

    public void renameSession(String sessionUuid, String title) {
        SessionRow session = repository.findSessionByUuid(sessionUuid);
        if (session != null && title != null && !title.isBlank()) {
            repository.updateSessionTitle(session.id(), title.trim());
        }
    }

    public Map<String, Object> getContext() {
        Map<String, Object> chartPayload = new HashMap<>();
        chartPayload.put("recent_task_anomaly", List.of());
        chartPayload.put("defect_category_samples", List.of());
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("chart_payload", chartPayload);
        ctx.put("recent_tasks", List.of());
        ctx.put("defect_library", Map.of("total_samples", 0, "categories", List.of()));
        return ctx;
    }

    public Map<String, Object> weeklyReportPreview() {
        Map<String, Object> data = new HashMap<>();
        data.put("week_start", "");
        data.put("week_end", "");
        data.put("kpi", Map.of());
        Map<String, Object> last6 = new HashMap<>();
        last6.put("tasks", List.of());
        last6.put("category_totals", List.of());
        data.put("last6_stats", last6);
        return data;
    }

    public void streamChat(String prompt, String sessionUuid, String model, SseEmitter emitter) {
        executor.submit(() -> {
            try {
                doStream(prompt, sessionUuid, model, emitter);
                emitter.complete();
            } catch (Exception e) {
                log.warn("Agent 流式对话异常: {}", e.getMessage());
                Map<String, Object> err = new HashMap<>();
                err.put("type", "error");
                err.put("message", e.getMessage() == null ? "Agent 调用失败" : e.getMessage());
                err.put("session_id", sessionUuid == null ? "" : sessionUuid);
                safeEmit(emitter, err);
                emitter.completeWithError(e);
            }
        });
    }

    private void doStream(String prompt, String sessionUuid, String model, SseEmitter emitter) throws Exception {
        Long sessionId;
        String activeUuid;
        String title;
        if (sessionUuid != null && !sessionUuid.isBlank()) {
            SessionRow s = repository.findSessionByUuid(sessionUuid);
            if (s != null) {
                sessionId = s.id();
                activeUuid = s.uuid();
                title = s.title();
            } else {
                activeUuid = UUID.randomUUID().toString();
                title = truncate(prompt, 48);
                sessionId = repository.createSession(activeUuid, title);
            }
        } else {
            activeUuid = UUID.randomUUID().toString();
            title = truncate(prompt, 48);
            sessionId = repository.createSession(activeUuid, title);
        }

        List<ChatMessage> history = repository.loadHistory(sessionId, 8);
        repository.insertMessage(sessionId, "user", prompt, null, null, null);
        repository.touchSession(sessionId);

        Map<String, Object> chartPayload = new HashMap<>();
        chartPayload.put("recent_task_anomaly", List.of());
        chartPayload.put("defect_category_samples", List.of());
        Map<String, Object> sessionEvent = new HashMap<>();
        sessionEvent.put("type", "session");
        sessionEvent.put("session_id", activeUuid);
        sessionEvent.put("title", title);
        sessionEvent.put("chart_payload", chartPayload);
        safeEmit(emitter, sessionEvent);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT
                + "\n\n[系统上下文] 当前接入的底层大模型：" + resolveModelLabel(model)
                + "。若用户询问你是什么模型，请如实告知该模型名。"));
        messages.addAll(history);
        messages.add(new ChatMessage("user", prompt));

        StringBuilder assistant = new StringBuilder();
        long start = System.currentTimeMillis();
        llmClient.streamChat(messages, model, new Consumer<String>() {
            @Override
            public void accept(String token) {
                assistant.append(token);
                Map<String, Object> tokenEvent = new HashMap<>();
                tokenEvent.put("type", "token");
                tokenEvent.put("content", token);
                safeEmit(emitter, tokenEvent);
            }
        });
        int latency = (int) (System.currentTimeMillis() - start);

        repository.insertMessage(sessionId, "assistant", assistant.toString(), "stub", assistant.length(), latency);
        repository.touchSession(sessionId);

        Map<String, Object> doneEvent = new HashMap<>();
        doneEvent.put("type", "done");
        doneEvent.put("session_id", activeUuid);
        doneEvent.put("chart_payload", chartPayload);
        safeEmit(emitter, doneEvent);
    }

    private void safeEmit(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
            // emitter 可能已关闭，忽略
        }
    }

    private String resolveModelLabel(String providerKey) {
        String key = (providerKey == null || providerKey.isBlank()) ? llmProperties.getDefaultProvider() : providerKey;
        AgentLlmProperties.Provider p = llmProperties.getProviders().get(key);
        if (p == null) {
            p = llmProperties.getProviders().get(llmProperties.getDefaultProvider());
        }
        if (p == null) {
            return key;
        }
        String label = p.getLabel() != null ? p.getLabel() : key;
        String model = p.getModel() != null ? p.getModel() : "";
        return label + (model.isBlank() ? "" : "（" + model + "）");
    }

    private static String truncate(String text, int max) {
        String clean = text == null ? "" : text.trim().replace("\n", " ");
        return clean.isEmpty() ? "新对话" : clean.substring(0, Math.min(clean.length(), max));
    }
}
