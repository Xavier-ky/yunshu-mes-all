package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.config.AgentLlmProperties;
import com.yunshu.mes.agent.dto.ChatStreamRequest;
import com.yunshu.mes.agent.dto.SessionRenameRequest;
import com.yunshu.mes.agent.service.AgentChatService;
import com.yunshu.mes.agent.vo.ChatMessageVO;
import com.yunshu.mes.agent.vo.ChatSessionVO;
import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 中心聊天接口，路径与移植的 AICommandCenter 前端约定一致。
 * 与其他业务模块解耦：仅依赖 {@link AgentChatService}，不直接调用其他模块。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentChatController {

    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final AgentChatService agentChatService;
    private final AgentLlmProperties llmProperties;

    public AgentChatController(AgentChatService agentChatService, AgentLlmProperties llmProperties) {
        this.agentChatService = agentChatService;
        this.llmProperties = llmProperties;
    }

    /** 可用模型列表，供前端模型选择器下拉 */
    @GetMapping("/models")
    public ApiResponse<List<Map<String, Object>>> models(HttpServletRequest request) {
        List<Map<String, Object>> list = new ArrayList<>();
        llmProperties.getProviders().forEach((key, p) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("label", p.getLabel() != null ? p.getLabel() : key);
            m.put("desc", p.getDesc() != null ? p.getDesc() : "");
            m.put("default", key.equals(llmProperties.getDefaultProvider()));
            list.add(m);
        });
        return ApiResponse.success(list, request);
    }

    @GetMapping("/chat/sessions")
    public ApiResponse<List<ChatSessionVO>> listSessions(
            @RequestParam(value = "limit", defaultValue = "30") int limit,
            HttpServletRequest request) {
        return ApiResponse.success(agentChatService.listSessions(limit), request);
    }

    @GetMapping("/chat/sessions/{id}/messages")
    public ApiResponse<List<ChatMessageVO>> listMessages(
            @PathVariable("id") String sessionId,
            @RequestParam(value = "limit", defaultValue = "300") int limit,
            HttpServletRequest request) {
        return ApiResponse.success(agentChatService.listMessages(sessionId, limit), request);
    }

    @DeleteMapping("/chat/sessions/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable("id") String sessionId, HttpServletRequest request) {
        agentChatService.deleteSession(sessionId);
        return ApiResponse.success(null, request);
    }

    @PatchMapping("/chat/sessions/{id}")
    public ApiResponse<Void> renameSession(@PathVariable("id") String sessionId,
                                           @RequestBody SessionRenameRequest body,
                                           HttpServletRequest request) {
        agentChatService.renameSession(sessionId, body == null ? null : body.title());
        return ApiResponse.success(null, request);
    }

    @PostMapping("/chat/stream")
    public SseEmitter stream(@RequestBody ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String prompt = request == null ? null : request.prompt();
        String sessionId = request == null ? null : request.session_id();
        String model = request == null ? null : request.model();
        agentChatService.streamChat(prompt, sessionId, model, emitter);
        return emitter;
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context(HttpServletRequest request) {
        return ApiResponse.success(agentChatService.getContext(), request);
    }

    @GetMapping("/weekly-report/preview")
    public ApiResponse<Map<String, Object>> weeklyReportPreview(HttpServletRequest request) {
        return ApiResponse.success(agentChatService.weeklyReportPreview(), request);
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<Map<String, String>> exportWeeklyReport(@RequestBody Map<String, Object> body) {
        // Stub：Word 周报导出未实现。接入文档生成后替换为返回 docx 二进制流。
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("detail", "周报导出未实现（Stub），待接入 Word 生成"));
    }
}
