package com.yunshu.mes.system.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class DepartmentController {

    private final JdbcTemplate jdbc;
    public DepartmentController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/departments")
    public ApiResponse<List<Map<String,Object>>> list(HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("SELECT dept_id, dept_code, dept_name, dept_type, status FROM sys_department ORDER BY dept_id"), req);
    }

    @PostMapping("/departments")
    public ApiResponse<Long> create(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        jdbc.update("INSERT INTO sys_department (dept_code, dept_name, dept_type, status) VALUES (?,?,?,?)",
            body.get("deptCode"), body.get("deptName"), body.getOrDefault("deptType",""), body.getOrDefault("status","ENABLED"));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return ApiResponse.success(id, req);
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String,Object> body, HttpServletRequest req) {
        jdbc.update("UPDATE sys_department SET dept_code=?, dept_name=?, dept_type=?, status=? WHERE dept_id=?",
            body.get("deptCode"), body.get("deptName"), body.get("deptType"), body.get("status"), id);
        return ApiResponse.success(null, req);
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        jdbc.update("DELETE FROM sys_department WHERE dept_id=?", id);
        return ApiResponse.success(null, req);
    }
}
