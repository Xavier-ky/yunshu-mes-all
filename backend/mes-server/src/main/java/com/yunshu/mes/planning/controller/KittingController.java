package com.yunshu.mes.planning.controller;

import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import com.yunshu.mes.inventory.workflow.InventoryBatchBridgeService;
import com.yunshu.mes.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/planning")
public class KittingController {

    private final JdbcTemplate jdbc;
    private final WorkOrderLifecycleService lifecycle;
    private final InventoryBatchBridgeService batchBridge;

    public KittingController(JdbcTemplate jdbc, WorkOrderLifecycleService lifecycle,
            InventoryBatchBridgeService batchBridge) {
        this.jdbc = jdbc;
        this.lifecycle = lifecycle;
        this.batchBridge = batchBridge;
    }

    @GetMapping("/kitting/{workOrderId}")
    public ApiResponse<Map<String,Object>> analyze(@PathVariable Long workOrderId, HttpServletRequest req) {
        // 1. Get work order
        var wo = jdbc.queryForMap("SELECT wo.*, p.product_name FROM work_order wo JOIN product p ON wo.product_id=p.product_id WHERE wo.work_order_id=?", workOrderId);
        if (wo.isEmpty()) return ApiResponse.fail("NOT_FOUND", "Work order not found", req);

        Long productId = ((Number) wo.get("product_id")).longValue();
        BigDecimal planQty = (BigDecimal) wo.get("plan_qty");

        // 2. Get BOM
        var boms = jdbc.queryForList("SELECT bom_id FROM bom WHERE product_id=?", productId);
        if (boms.isEmpty()) return ApiResponse.fail("NO_BOM", "No BOM found", req);
        Long bomId = ((Number) boms.get(0).get("bom_id")).longValue();

        // 3. Get BOM items
        var items = jdbc.queryForList("""
            SELECT bi.*, m.material_code, m.material_name, m.is_key_material
            FROM bom_item bi JOIN material m ON bi.material_id=m.material_id
            WHERE bi.bom_id=? ORDER BY bi.bom_item_id""", bomId);

        // 4. Calculate required vs available
        List<Map<String,Object>> shortages = new ArrayList<>();
        List<Map<String,Object>> details = new ArrayList<>();
        boolean allSufficient = true;

        for (var item : items) {
            Long materialId = ((Number) item.get("material_id")).longValue();
            BigDecimal qtyPer = (BigDecimal) item.get("qty_per");
            BigDecimal lossRate = (BigDecimal) item.get("loss_rate");
            BigDecimal required = planQty.multiply(qtyPer).multiply(BigDecimal.ONE.add(lossRate));

            BigDecimal available = batchBridge.availableQtyForMaterial(materialId);

            BigDecimal shortage = required.subtract(available);
            boolean sufficient = shortage.compareTo(BigDecimal.ZERO) <= 0;

            Map<String,Object> detail = new LinkedHashMap<>();
            detail.put("materialId", materialId);
            detail.put("materialCode", item.get("material_code"));
            detail.put("materialName", item.get("material_name"));
            detail.put("isKey", item.get("is_key_material"));
            detail.put("qtyPer", qtyPer);
            detail.put("lossRate", lossRate);
            detail.put("required", required);
            detail.put("available", available);
            detail.put("shortage", sufficient ? BigDecimal.ZERO : shortage);
            detail.put("sufficient", sufficient);
            details.add(detail);

            if (!sufficient) {
                allSufficient = false;
                Map<String,Object> s = new LinkedHashMap<>();
                s.put("materialId", materialId);
                s.put("materialCode", item.get("material_code"));
                s.put("materialName", item.get("material_name"));
                s.put("shortageQty", shortage);
                shortages.add(s);
            }
        }

        // 5. Record kitting_analysis
        jdbc.update("INSERT INTO kitting_analysis (work_order_id, analysis_status, required_summary, available_summary, analyzed_by) VALUES (?,?,?,?,?)",
            workOrderId, allSufficient ? "SUFFICIENT" : "INSUFFICIENT",
            "{}", "{}", 1);

        Long analysisId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 6. Record material_shortage if any
        for (var s : shortages) {
            jdbc.update("INSERT INTO material_shortage (analysis_id, material_id, required_qty, available_qty, shortage_qty, status) VALUES (?,?,?,?,?,?)",
                analysisId, s.get("materialId"), BigDecimal.ZERO, BigDecimal.ZERO, s.get("shortageQty"), "OPEN");
        }

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("workOrderId", workOrderId);
        result.put("workOrderNo", wo.get("work_order_no"));
        result.put("productName", wo.get("product_name"));
        result.put("planQty", planQty);
        result.put("allSufficient", allSufficient);
        result.put("details", details);
        result.put("shortages", shortages);
        result.put("analysisId", analysisId);
        return ApiResponse.success(result, req);
    }

    @PostMapping("/kitting/{workOrderId}/reserve")
    public ApiResponse<Void> reserve(@PathVariable Long workOrderId, HttpServletRequest req) {
        // Lock inventory for all BOM materials
        var wo = jdbc.queryForMap("SELECT product_id, plan_qty FROM work_order WHERE work_order_id=?", workOrderId);
        Long productId = ((Number) wo.get("product_id")).longValue();
        BigDecimal planQty = (BigDecimal) wo.get("plan_qty");

        var boms = jdbc.queryForList("SELECT bom_id FROM bom WHERE product_id=?", productId);
        if (boms.isEmpty()) return ApiResponse.fail("NO_BOM","No BOM",req);
        Long bomId = ((Number) boms.get(0).get("bom_id")).longValue();

        var items = jdbc.queryForList("SELECT bi.* FROM bom_item bi WHERE bi.bom_id=?", bomId);
        for (var item : items) {
            Long materialId = ((Number) item.get("material_id")).longValue();
            batchBridge.ensureMaterialStockBridged(materialId);
            BigDecimal qtyPer = (BigDecimal) item.get("qty_per");
            BigDecimal lossRate = (BigDecimal) item.get("loss_rate");
            BigDecimal need = planQty.multiply(qtyPer).multiply(BigDecimal.ONE.add(lossRate));

            // Lock from batches, FIFO
            var batches = jdbc.queryForList(
                "SELECT batch_id, available_qty, locked_qty FROM inventory_batch WHERE material_id=? AND status='IN_STOCK' ORDER BY batch_id", materialId);
            BigDecimal remaining = need;
            for (var batch : batches) {
                Long batchId = ((Number) batch.get("batch_id")).longValue();
                BigDecimal avail = ((BigDecimal) batch.get("available_qty")).subtract((BigDecimal) batch.get("locked_qty"));
                if (avail.compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal lock = remaining.compareTo(avail) < 0 ? remaining : avail;
                jdbc.update("UPDATE inventory_batch SET locked_qty = locked_qty + ? WHERE batch_id=?", lock, batchId);
                remaining = remaining.subtract(lock);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            }
        }
        jdbc.update("UPDATE work_order SET status='DISPATCHED' WHERE work_order_id=?", workOrderId);
        lifecycle.advanceIfNullOrEarlier(workOrderId, WorkOrderLifecycleService.KITTING_OK);
        return ApiResponse.success(null, req);
    }

    @PostMapping("/kitting/{workOrderId}/release")
    public ApiResponse<Void> release(@PathVariable Long workOrderId, HttpServletRequest req) {
        var wo = jdbc.queryForMap("SELECT product_id, plan_qty FROM work_order WHERE work_order_id=?", workOrderId);
        if (wo.isEmpty()) return ApiResponse.fail("NOT_FOUND","WO not found",req);
        Long productId = ((Number) wo.get("product_id")).longValue();
        BigDecimal planQty = (BigDecimal) wo.get("plan_qty");
        var boms = jdbc.queryForList("SELECT bom_id FROM bom WHERE product_id=?", productId);
        if (boms.isEmpty()) return ApiResponse.success(null, req);
        Long bomId = ((Number) boms.get(0).get("bom_id")).longValue();
        var items = jdbc.queryForList("SELECT bi.* FROM bom_item bi WHERE bi.bom_id=?", bomId);
        for (var item : items) {
            Long materialId = ((Number) item.get("material_id")).longValue();
            BigDecimal qtyPer = (BigDecimal) item.get("qty_per");
            BigDecimal lossRate = (BigDecimal) item.get("loss_rate");
            BigDecimal need = planQty.multiply(qtyPer).multiply(BigDecimal.ONE.add(lossRate));
            var batches = jdbc.queryForList(
                "SELECT batch_id, locked_qty FROM inventory_batch WHERE material_id=? AND locked_qty>0 ORDER BY batch_id DESC", materialId);
            BigDecimal remaining = need;
            for (var batch : batches) {
                Long batchId = ((Number) batch.get("batch_id")).longValue();
                BigDecimal locked = (BigDecimal) batch.get("locked_qty");
                BigDecimal release = remaining.compareTo(locked) < 0 ? remaining : locked;
                jdbc.update("UPDATE inventory_batch SET locked_qty = locked_qty - ? WHERE batch_id=?", release, batchId);
                remaining = remaining.subtract(release);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            }
        }
        return ApiResponse.success(null, req);
    }
}
