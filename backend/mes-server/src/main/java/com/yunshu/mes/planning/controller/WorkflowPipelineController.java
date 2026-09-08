package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import com.yunshu.mes.planning.workflow.WorkflowPipelineService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planning/workflow")
public class WorkflowPipelineController {

    private final WorkflowPipelineService pipelineService;

    public WorkflowPipelineController(WorkflowPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/todos")
    public ApiResponse<List<Map<String, Object>>> todos(
            @RequestParam(value = "role", required = false) String role,
            HttpServletRequest request) {
        String effectiveRole = role;
        if (effectiveRole == null || effectiveRole.isBlank()) {
            effectiveRole = firstRole(request);
        }
        return ApiResponse.success(pipelineService.todosForRole(effectiveRole), request);
    }

    @GetMapping("/pipeline/{key}")
    public ApiResponse<Map<String, Object>> pipeline(
            @PathVariable String key,
            HttpServletRequest request) {
        return ApiResponse.success(pipelineService.pipelineByKey(key), request);
    }

    @SuppressWarnings("unchecked")
    private static String firstRole(HttpServletRequest request) {
        Object roles = request.getAttribute("currentRoles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        if (roles instanceof String s && !s.isBlank()) {
            return s.split(",")[0].trim();
        }
        return "";
    }
}
