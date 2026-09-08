package com.yunshu.mes.system.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class OperationLogController {

    private final JdbcTemplate jdbc;
    public OperationLogController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/logs")
    public ApiResponse<List<Map<String,Object>>> list(HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("""
            SELECT ol.log_id, ol.user_id, u.real_name as user_name, ol.operation_type,
                   ol.biz_object_type, ol.biz_object_id, ol.result_status, ol.result_message, ol.operation_time
            FROM sys_operation_log ol LEFT JOIN sys_user u ON ol.user_id=u.user_id
            ORDER BY ol.operation_time DESC LIMIT 200"""), req);
    }
}
