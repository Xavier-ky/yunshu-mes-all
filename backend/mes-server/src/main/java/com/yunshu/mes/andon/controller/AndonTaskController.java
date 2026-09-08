package com.yunshu.mes.andon.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/andon")
public class AndonTaskController {
    private final JdbcTemplate jdbc;
    public AndonTaskController(JdbcTemplate j) { this.jdbc = j; }

    @PostMapping("/events/{andonId}/assign")
    public ApiResponse<Void> assign(@PathVariable Long andonId, @RequestBody Map<String,Object> body, HttpServletRequest req) {
        Long handlerId = Long.valueOf(body.get("handlerId").toString());
        jdbc.update("INSERT INTO andon_task (andon_id, handler_id, status) VALUES (?,?,?)", andonId, handlerId, "ASSIGNED");
        jdbc.update("INSERT INTO andon_notice (andon_id, receiver_id, notice_channel) VALUES (?,?,?)", andonId, handlerId, "IN_APP");
        jdbc.update("UPDATE andon_event SET status='PROCESSING' WHERE andon_id=?", andonId);
        return ApiResponse.success(null, req);
    }

    @PostMapping("/events/{andonId}/result")
    public ApiResponse<Void> result(@PathVariable Long andonId, @RequestBody Map<String,Object> body, HttpServletRequest req) {
        String measure = (String) body.getOrDefault("handleMeasure", body.getOrDefault("resolutionResult", ""));
        String desc = (String) body.getOrDefault("resultDesc", body.getOrDefault("remark", ""));
        Object closeUser = body.get("closeUserId");
        jdbc.update("""
                INSERT INTO andon_result (andon_task_id, handle_measure, result_desc, close_user_id, close_time)
                SELECT andon_task_id, ?, ?, ?, NOW(3) FROM andon_task WHERE andon_id=? ORDER BY andon_task_id DESC LIMIT 1
                """, measure, desc, closeUser, andonId);
        jdbc.update("UPDATE andon_event SET status=?, close_time=NOW(3) WHERE andon_id=?",
            body.getOrDefault("status", "CLOSED"), andonId);
        return ApiResponse.success(null, req);
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String,Object>>> listTasks(HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("""
            SELECT at.*, ae.andon_no, ae.exception_desc, u.real_name as handler_name
            FROM andon_task at JOIN andon_event ae ON at.andon_id=ae.andon_id
            LEFT JOIN sys_user u ON at.handler_id=u.user_id ORDER BY at.andon_task_id DESC"""), req);
    }
}
