package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.service.AgentSchedulingService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Explicit, JWT-protected write boundary for the autonomous scheduling graph. */
@RestController
@RequestMapping("/api/agent/scheduling")
public class AgentSchedulingController {

    private static final Set<String> SCHEDULING_ROLES = Set.of("MANAGER", "PROD_SUPERVISOR", "TESTER");
    private final AgentSchedulingService service;

    public AgentSchedulingController(AgentSchedulingService service) {
        this.service = service;
    }

    @GetMapping("/work-orders/{workOrderId}/advisory")
    public ApiResponse<Map<String, Object>> advisory(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request);
        return ApiResponse.success(service.advisory(workOrderId), request);
    }

    @PostMapping("/work-orders/{workOrderId}/execute")
    public ApiResponse<Map<String, Object>> execute(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request);
        return ApiResponse.success(service.execute(workOrderId, userId(request)), request);
    }

    @SuppressWarnings("unchecked")
    private static void requireRole(HttpServletRequest request) {
        if (request.getAttribute("currentUser") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MES JWT is required for scheduling Agent");
        }
        Object rawRoles = request.getAttribute("currentRoles");
        Collection<?> values = rawRoles instanceof Collection<?> collection
                ? collection : rawRoles == null ? List.of() : List.of(String.valueOf(rawRoles).split(","));
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) roles.add(String.valueOf(value).trim().toUpperCase());
        }
        if (roles.stream().noneMatch(SCHEDULING_ROLES::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current role cannot execute scheduling Agent");
        }
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
