package com.yunshu.mes.production.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/binding-ops")
public class MaterialBindingController {
    private final JdbcTemplate jdbc;
    public MaterialBindingController(JdbcTemplate j) { this.jdbc = j; }

    @PostMapping("/material-bindings")
    public ApiResponse<Void> bind(@RequestBody Map<String,Object> body, HttpServletRequest req) {
        Long snId = Long.valueOf(body.get("snId").toString());
        Long batchId = Long.valueOf(body.get("batchId").toString());
        Long materialId = Long.valueOf(body.get("materialId").toString());
        Long stepId = body.get("stepId")!=null?Long.valueOf(body.get("stepId").toString()):null;
        Long userId = Long.valueOf(body.get("bindUserId").toString());
        Long reportId = body.get("reportId") != null
                ? Long.valueOf(body.get("reportId").toString()) : resolveLatestReport(snId);
        jdbc.update("""
                INSERT INTO product_material_binding
                (sn_id, batch_id, material_id, step_id, report_id, bind_user_id)
                VALUES (?,?,?,?,?,?)
                """, snId, batchId, materialId, stepId, reportId, userId);
        return ApiResponse.success(null, req);
    }

    private Long resolveLatestReport(Long snId) {
        return jdbc.query("""
                SELECT pr.report_id FROM production_report pr
                JOIN product_sn ps ON ps.work_order_id = pr.work_order_id
                WHERE ps.sn_id = ?
                ORDER BY pr.report_time DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("report_id") : null, snId);
    }

    @GetMapping("/material-bindings/{snId}")
    public ApiResponse<List<Map<String,Object>>> list(@PathVariable Long snId, HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("""
            SELECT pmb.*, ib.batch_no, m.material_code, m.material_name
            FROM product_material_binding pmb
            LEFT JOIN inventory_batch ib ON pmb.batch_id=ib.batch_id
            LEFT JOIN material m ON pmb.material_id=m.material_id
            WHERE pmb.sn_id=?""", snId), req);
    }
}
