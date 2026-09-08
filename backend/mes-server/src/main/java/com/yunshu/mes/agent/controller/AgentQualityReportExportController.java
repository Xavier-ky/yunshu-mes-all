package com.yunshu.mes.agent.controller;

import com.yunshu.mes.agent.service.AgentQualityReportExportService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT-owned DOCX export for a frozen QualityReportAgent artifact. */
@RestController
@RequestMapping("/api/agent/quality/reports")
public class AgentQualityReportExportController {
    private static final Set<String> ALLOWED = Set.of("MANAGER", "PROD_SUPERVISOR", "QUALITY_INSPECTOR", "TESTER");
    private final AgentQualityReportExportService service;

    public AgentQualityReportExportController(AgentQualityReportExportService service) { this.service = service; }

    @PostMapping("/{reportId}/export")
    public ResponseEntity<byte[]> export(@PathVariable String reportId, HttpServletRequest request) {
        requireRole(request);
        Object rawUserId = request.getAttribute("currentUserId");
        if (rawUserId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少当前用户标识");
        Long userId;
        try { userId = Long.valueOf(String.valueOf(rawUserId)); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前用户标识无效"); }
        byte[] file = service.export(userId, reportId);
        String filename = "质量分析报告_" + java.time.LocalDate.now().toString().replace("-", "") + ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(file);
    }

    @SuppressWarnings("unchecked")
    private static void requireRole(HttpServletRequest request) {
        if (request.getAttribute("currentUser") == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agent 工具需要有效 MES JWT");
        Object raw = request.getAttribute("currentRoles");
        Collection<?> roles = raw instanceof Collection<?> c ? c : raw == null ? List.of() : List.of(String.valueOf(raw).split(","));
        boolean allowed = roles.stream().filter(java.util.Objects::nonNull).map(x -> String.valueOf(x).trim().toUpperCase()).anyMatch(ALLOWED::contains);
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权导出质量报告");
    }
}
