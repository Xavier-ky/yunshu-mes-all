package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.service.AgentReadService;
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

/** JWT-only, side-effect-free MES facts for the Agent tool gateway. */
@RestController
@RequestMapping("/api/agent/read")
public class AgentReadController {

    private static final Set<String> BUSINESS_ROLES = Set.of(
            "MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "LINE_OPERATOR", "QUALITY_INSPECTOR", "TESTER");
    private static final Set<String> MATERIAL_ROLES = Set.of("MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "TESTER");
    private static final Set<String> TRACE_ROLES = Set.of("MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "QUALITY_INSPECTOR", "TESTER");

    private final AgentReadService service;

    public AgentReadController(AgentReadService service) {
        this.service = service;
    }

    @GetMapping("/work-orders/{workOrderNo}/pipeline")
    public ApiResponse<Map<String, Object>> pipeline(@PathVariable String workOrderNo, HttpServletRequest request) {
        requireRole(request, BUSINESS_ROLES);
        Map<String, Object> data = service.pipeline(workOrderNo);
        return data.isEmpty() ? ApiResponse.fail("NOT_FOUND", "工单不存在", request) : ApiResponse.success(data, request);
    }

    @GetMapping("/work-orders/{workOrderId}/kitting")
    public ApiResponse<Map<String, Object>> kitting(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request, MATERIAL_ROLES);
        Map<String, Object> data = service.kitting(workOrderId);
        return data.isEmpty() ? ApiResponse.fail("NOT_FOUND", "工单不存在", request) : ApiResponse.success(data, request);
    }

    @GetMapping("/work-orders/{workOrderId}/bom-route")
    public ApiResponse<Map<String, Object>> bomRoute(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request, MATERIAL_ROLES);
        Map<String, Object> data = service.bomRoute(workOrderId);
        return data.isEmpty()
                ? ApiResponse.fail("NOT_FOUND", "Work order not found", request)
                : ApiResponse.success(data, request);
    }

    @GetMapping("/work-orders/{workOrderId}/tasks")
    public ApiResponse<Map<String, Object>> tasks(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request, BUSINESS_ROLES);
        return ApiResponse.success(service.tasks(workOrderId), request);
    }

    @GetMapping("/work-orders/{workOrderId}/dispatches")
    public ApiResponse<Map<String, Object>> dispatches(@PathVariable Long workOrderId, HttpServletRequest request) {
        Set<String> roles = requireRole(request, BUSINESS_ROLES);
        Long operatorId = roles.contains("LINE_OPERATOR") && !roles.contains("MANAGER") && !roles.contains("PROD_SUPERVISOR")
                ? userId(request) : null;
        return ApiResponse.success(service.dispatches(workOrderId, operatorId), request);
    }

    @GetMapping("/work-orders/{workOrderNo}/trace")
    public ApiResponse<Map<String, Object>> trace(@PathVariable String workOrderNo, HttpServletRequest request) {
        requireRole(request, TRACE_ROLES);
        Map<String, Object> data = service.trace(workOrderNo);
        return data.isEmpty() ? ApiResponse.fail("NOT_FOUND", "工单不存在", request) : ApiResponse.success(data, request);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> requireRole(HttpServletRequest request, Set<String> accepted) {
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
        if (roles.stream().noneMatch(accepted::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权调用此 Agent 工具");
        }
        return roles;
    }

    private static Long userId(HttpServletRequest request) {
        Object value = request.getAttribute("currentUserId");
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
