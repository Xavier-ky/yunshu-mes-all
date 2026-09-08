package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.service.AgentQualityReadService;
import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT-protected, side-effect-free quality facts for the QualityManagementAgent. */
@RestController
@RequestMapping("/api/agent/read")
public class AgentQualityReadController {

    private static final Set<String> QUALITY_READ_ROLES = Set.of(
            "MANAGER", "PROD_SUPERVISOR", "QUALITY_INSPECTOR", "TESTER");

    private final AgentQualityReadService service;

    public AgentQualityReadController(AgentQualityReadService service) {
        this.service = service;
    }

    @GetMapping("/quality/overview")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        requireRole(request);
        return ApiResponse.success(service.overview(days), request);
    }

    /**
     * One immutable-at-read-time fact package for the conversational quality
     * report. It is intentionally read-only and separate from page endpoints.
     */
    @GetMapping("/quality/report-snapshot")
    public ApiResponse<Map<String, Object>> reportSnapshot(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        requireRole(request);
        return ApiResponse.success(service.reportSnapshot(days), request);
    }

    @GetMapping("/work-orders/{workOrderNo}/quality")
    public ApiResponse<Map<String, Object>> workOrderQuality(
            @PathVariable String workOrderNo,
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        requireRole(request);
        Map<String, Object> data = service.workOrderQuality(workOrderNo, days);
        return data.isEmpty()
                ? ApiResponse.fail("NOT_FOUND", "工单不存在", request)
                : ApiResponse.success(data, request);
    }

    @SuppressWarnings("unchecked")
    private static void requireRole(HttpServletRequest request) {
        if (request.getAttribute("currentUser") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agent 工具需要有效的 MES JWT");
        }
        Object rawRoles = request.getAttribute("currentRoles");
        Collection<?> values = rawRoles instanceof Collection<?> collection
                ? collection
                : rawRoles == null ? List.of() : List.of(String.valueOf(rawRoles).split(","));
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) roles.add(String.valueOf(value).trim().toUpperCase());
        }
        if (roles.stream().noneMatch(QUALITY_READ_ROLES::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权调用质量 Agent 只读工具");
        }
    }
}
