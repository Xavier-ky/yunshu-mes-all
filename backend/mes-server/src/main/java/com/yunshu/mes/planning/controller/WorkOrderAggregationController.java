package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planning")
public class WorkOrderAggregationController {
    private final JdbcTemplate jdbc;
    public WorkOrderAggregationController(JdbcTemplate j) { this.jdbc = j; }

    @GetMapping("/work-orders/{id}/progress")
    public ApiResponse<?> progress(@PathVariable Long id, HttpServletRequest req) {
        // Aggregate across all production lines for this work order
        var wo = jdbc.queryForMap("SELECT wo.*, p.product_name FROM work_order wo JOIN product p ON wo.product_id=p.product_id WHERE wo.work_order_id=?", id);
        if (wo.isEmpty()) return ApiResponse.fail("NOT_FOUND","WO not found",req);

        var lines = jdbc.queryForList("""
            SELECT pt.line_id, l.line_name, pt.task_qty, pt.completed_qty, pt.status,
                   ROUND(pt.completed_qty/pt.task_qty*100,1) as progress_pct
            FROM production_task pt JOIN production_line l ON pt.line_id=l.line_id
            WHERE pt.work_order_id=? ORDER BY pt.task_id""", id);

        var total = jdbc.queryForMap(
            "SELECT COALESCE(SUM(task_qty),0) as total_qty, COALESCE(SUM(completed_qty),0) as total_completed FROM production_task WHERE work_order_id=?", id);

        var result = new java.util.LinkedHashMap<>();
        result.put("workOrder", wo);
        result.put("lineDetails", lines);
        result.put("totalQty", total.get("total_qty"));
        result.put("totalCompleted", total.get("total_completed"));
        return ApiResponse.success(result, req);
    }
}
