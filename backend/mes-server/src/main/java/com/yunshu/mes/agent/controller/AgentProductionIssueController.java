package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.service.AgentProductionIssueService;
import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT-protected write endpoint for the single ProductionIssueAgent business boundary. */
@RestController
@RequestMapping("/api/agent/production-issue")
public class AgentProductionIssueController {

    private static final Set<String> ISSUE_ROLES = Set.of("MANAGER", "PROD_SUPERVISOR", "WAREHOUSE_CLERK", "TESTER");
    private final AgentProductionIssueService service;

    public AgentProductionIssueController(AgentProductionIssueService service) {
        this.service = service;
    }

    @PostMapping("/work-orders/{workOrderId}/execute")
    public ApiResponse<Map<String, Object>> execute(@PathVariable Long workOrderId, HttpServletRequest request) {
        requireRole(request);
        return ApiResponse.success(service.execute(workOrderId), request);
    }

    private static void requireRole(HttpServletRequest request) {
        if (request.getAttribute("currentUser") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MES JWT is required for ProductionIssueAgent");
        }
        Object rawRoles = request.getAttribute("currentRoles");
        Collection<?> values = rawRoles instanceof Collection<?> collection
                ? collection : rawRoles == null ? List.of() : List.of(String.valueOf(rawRoles).split(","));
        Set<String> roles = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) roles.add(String.valueOf(value).trim().toUpperCase());
        }
        if (roles.stream().noneMatch(ISSUE_ROLES::contains)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current role cannot execute ProductionIssueAgent");
        }
    }
}
