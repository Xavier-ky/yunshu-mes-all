package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planning")
public class TaskLogController {
    private final JdbcTemplate jdbc;
    public TaskLogController(JdbcTemplate j) { this.jdbc = j; }

    @PostMapping("/task-logs")
    public ApiResponse<Void> logOperation(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        jdbc.update("INSERT INTO task_operation_log (task_id, dispatch_id, operation_type, operator_id) VALUES (?,?,?,?)",
            body.get("taskId")!=null?Long.valueOf(body.get("taskId").toString()):null,
            body.get("dispatchId")!=null?Long.valueOf(body.get("dispatchId").toString()):null,
            body.get("operationType"), Long.valueOf(body.get("operatorId").toString()));
        return ApiResponse.success(null, req);
    }

    @GetMapping("/task-logs/{taskId}")
    public ApiResponse<List<Map<String,Object>>> listLogs(@PathVariable Long taskId, HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("""
            SELECT tl.*, u.real_name as operator_name
            FROM task_operation_log tl LEFT JOIN sys_user u ON tl.operator_id=u.user_id
            WHERE tl.task_id=? ORDER BY tl.log_id DESC""", taskId), req);
    }
}
