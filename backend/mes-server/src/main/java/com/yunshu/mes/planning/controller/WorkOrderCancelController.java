package com.yunshu.mes.planning.controller;

import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planning")
public class WorkOrderCancelController {
    private final JdbcTemplate jdbc;
    public WorkOrderCancelController(JdbcTemplate j) { this.jdbc = j; }

    @PostMapping("/work-orders/{id}/cancel")
    public ApiResponse<Map<String,Object>> cancel(@PathVariable Long id, HttpServletRequest req) {
        var wo = jdbc.queryForMap("SELECT * FROM work_order WHERE work_order_id=?", id);
        if (wo.isEmpty()) return ApiResponse.fail("NOT_FOUND","WO not found",req);
        String currentStatus = (String) wo.get("status");
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            return ApiResponse.fail("INVALID_STATUS","Cannot cancel "+currentStatus+" work order",req);
        }

        // Release locked inventory for this WO's BOM materials
        Long productId = (Long) wo.get("product_id");
        BigDecimal planQty = (BigDecimal) wo.get("plan_qty");
        var boms = jdbc.queryForList("SELECT bom_id FROM bom WHERE product_id=?", productId);
        if (!boms.isEmpty()) {
            Long bomId = (Long) boms.get(0).get("bom_id");
            var items = jdbc.queryForList("SELECT bi.* FROM bom_item bi WHERE bi.bom_id=?", bomId);
            for (var item : items) {
                Long matId = (Long) item.get("material_id");
                BigDecimal qtyPer = (BigDecimal) item.get("qty_per");
                BigDecimal lossRate = (BigDecimal) item.get("loss_rate");
                BigDecimal need = planQty.multiply(qtyPer).multiply(BigDecimal.ONE.add(lossRate));
                var batches = jdbc.queryForList("SELECT batch_id,locked_qty FROM inventory_batch WHERE material_id=? AND locked_qty>0 ORDER BY batch_id DESC", matId);
                BigDecimal remaining = need;
                for (var batch : batches) {
                    Long bId = (Long) batch.get("batch_id");
                    BigDecimal locked = (BigDecimal) batch.get("locked_qty");
                    BigDecimal release = remaining.compareTo(locked) < 0 ? remaining : locked;
                    jdbc.update("UPDATE inventory_batch SET locked_qty=locked_qty-? WHERE batch_id=?", release, bId);
                    remaining = remaining.subtract(release);
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                }
            }
        }

        jdbc.update("UPDATE work_order SET status='CANCELLED' WHERE work_order_id=?", id);

        Map<String,Object> r = new LinkedHashMap<>();
        r.put("workOrderId", id); r.put("previousStatus", currentStatus); r.put("newStatus", "CANCELLED");
        r.put("inventoryReleased", true);
        return ApiResponse.success(r, req);
    }

    @GetMapping("/material-shortages")
    public ApiResponse<List<Map<String,Object>>> listShortages(HttpServletRequest req) {
        return ApiResponse.success(jdbc.queryForList("""
            SELECT ms.*, m.material_code, m.material_name, ka.work_order_id, wo.work_order_no
            FROM material_shortage ms
            JOIN material m ON ms.material_id=m.material_id
            JOIN kitting_analysis ka ON ms.analysis_id=ka.analysis_id
            JOIN work_order wo ON ka.work_order_id=wo.work_order_id
            WHERE ms.status='OPEN' ORDER BY ms.shortage_id DESC"""), req);
    }

    @PostMapping("/material-shortages/{id}/resolve")
    public ApiResponse<Void> resolveShortage(@PathVariable Long id, HttpServletRequest req) {
        jdbc.update("UPDATE material_shortage SET status='RESOLVED' WHERE shortage_id=?", id);
        return ApiResponse.success(null, req);
    }
}
