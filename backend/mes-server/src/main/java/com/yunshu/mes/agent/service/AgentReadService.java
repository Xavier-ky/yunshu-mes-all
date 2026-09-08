package com.yunshu.mes.agent.service;

import com.yunshu.mes.planning.compat.service.ProTaskService;
import com.yunshu.mes.planning.workflow.WorkflowPipelineService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Narrow, read-only fact facade for the Agent service.
 *
 * <p>This class must never call lifecycle transitions, trace synchronization,
 * stock locking or any write-capable compat controller. It is intentionally
 * separate from page APIs whose GET handlers may write analytical snapshots.
 */
@Service
public class AgentReadService {

    private final JdbcTemplate jdbc;
    private final WorkflowPipelineService pipelineService;
    private final ProTaskService taskService;

    public AgentReadService(
            JdbcTemplate jdbc,
            WorkflowPipelineService pipelineService,
            ProTaskService taskService) {
        this.jdbc = jdbc;
        this.pipelineService = pipelineService;
        this.taskService = taskService;
    }

    public Map<String, Object> pipeline(String workOrderNo) {
        return snapshot(pipelineService.pipelineByKey(workOrderNo));
    }

    /** Pure availability calculation; unlike KittingController it creates no analysis or shortage row. */
    public Map<String, Object> kitting(Long workOrderId) {
        List<Map<String, Object>> workOrders = jdbc.queryForList("""
                SELECT wo.work_order_id, wo.work_order_no, wo.plan_qty, wo.lifecycle_status,
                       p.product_name
                FROM work_order wo
                JOIN product p ON p.product_id = wo.product_id
                WHERE wo.work_order_id = ? AND wo.is_deleted = 0
                """, workOrderId);
        if (workOrders.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> workOrder = workOrders.get(0);
        Long productId = number(workOrder.get("product_id"));
        if (productId == null) {
            productId = jdbc.query("SELECT product_id FROM work_order WHERE work_order_id = ?", rs ->
                    rs.next() ? rs.getLong("product_id") : null, workOrderId);
        }
        List<Map<String, Object>> boms = jdbc.queryForList(
                "SELECT bom_id FROM bom WHERE product_id = ? AND status IN ('RELEASED', 'ENABLED') ORDER BY bom_id DESC LIMIT 1", productId);
        if (boms.isEmpty()) {
            return snapshot(Map.of(
                    "workOrderId", workOrderId,
                    "workOrderNo", workOrder.get("work_order_no"),
                    "allSufficient", false,
                    "reason", "NO_BOM",
                    "details", List.of(),
                    "shortages", List.of()));
        }
        Long bomId = number(boms.get(0).get("bom_id"));
        BigDecimal planQty = decimal(workOrder.get("plan_qty"));
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT bi.material_id, bi.qty_per, bi.loss_rate,
                       m.material_code, m.material_name, m.is_key_material
                FROM bom_item bi
                JOIN material m ON m.material_id = bi.material_id
                WHERE bi.bom_id = ?
                ORDER BY bi.bom_item_id
                """, bomId);
        List<Map<String, Object>> details = new ArrayList<>();
        List<Map<String, Object>> shortages = new ArrayList<>();
        boolean allSufficient = true;
        for (Map<String, Object> item : items) {
            Long materialId = number(item.get("material_id"));
            BigDecimal required = planQty.multiply(decimal(item.get("qty_per")))
                    .multiply(BigDecimal.ONE.add(decimal(item.get("loss_rate"))));
            BigDecimal available = jdbc.query("""
                    SELECT COALESCE(SUM(available_qty - locked_qty), 0) AS available
                    FROM inventory_batch
                    WHERE material_id = ? AND status = 'IN_STOCK' AND quality_status = 'QUALIFIED'
                    """, rs -> rs.next() ? decimal(rs.getBigDecimal("available")) : BigDecimal.ZERO, materialId);
            BigDecimal shortage = required.subtract(available).max(BigDecimal.ZERO);
            boolean sufficient = shortage.signum() == 0;
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("materialId", materialId);
            detail.put("materialCode", item.get("material_code"));
            detail.put("materialName", item.get("material_name"));
            detail.put("isKeyMaterial", item.get("is_key_material"));
            detail.put("requiredQty", required);
            detail.put("availableQty", available);
            detail.put("shortageQty", shortage);
            detail.put("sufficient", sufficient);
            details.add(detail);
            if (!sufficient) {
                allSufficient = false;
                shortages.add(detail);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrderId", workOrderId);
        result.put("workOrderNo", workOrder.get("work_order_no"));
        result.put("productName", workOrder.get("product_name"));
        result.put("lifecycleStatus", workOrder.get("lifecycle_status"));
        result.put("planQty", planQty);
        result.put("allSufficient", allSufficient);
        result.put("details", details);
        result.put("shortages", shortages);
        return snapshot(result);
    }

    /**
     * Resolves the manufacturing definition actually applicable to one work order.
     * A work-order-specific BOM/route is always preferred; otherwise the latest
     * released BOM and enabled default product route are used. This is a pure
     * read model for the BOM & Route Agent, never a route-release operation.
     */
    public Map<String, Object> bomRoute(Long workOrderId) {
        List<Map<String, Object>> workOrders = jdbc.queryForList("""
                SELECT wo.work_order_id, wo.work_order_no, wo.product_id, wo.bom_id, wo.route_id,
                       wo.plan_qty, wo.lifecycle_status, p.product_code, p.product_name
                FROM work_order wo
                JOIN product p ON p.product_id = wo.product_id
                WHERE wo.work_order_id = ? AND wo.is_deleted = 0
                """, workOrderId);
        if (workOrders.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> workOrder = workOrders.get(0);
        Long productId = number(workOrder.get("product_id"));
        Long assignedBomId = number(workOrder.get("bom_id"));
        Long assignedRouteId = number(workOrder.get("route_id"));

        List<Map<String, Object>> boms = assignedBomId == null
                ? jdbc.queryForList("""
                        SELECT bom_id, bom_code, bom_name, version_no, status
                        FROM bom
                        WHERE product_id = ? AND status IN ('RELEASED', 'ENABLED')
                        ORDER BY bom_id DESC
                        LIMIT 1
                        """, productId)
                : jdbc.queryForList("""
                        SELECT bom_id, bom_code, bom_name, version_no, status
                        FROM bom WHERE bom_id = ?
                        """, assignedBomId);
        Map<String, Object> bom = boms.isEmpty() ? null : boms.get(0);
        Long bomId = bom == null ? null : number(bom.get("bom_id"));
        List<Map<String, Object>> materials = bomId == null ? List.of() : jdbc.queryForList("""
                SELECT bi.material_id AS materialId, m.material_code AS materialCode,
                       m.material_name AS materialName, bi.qty_per AS qtyPer,
                       bi.loss_rate AS lossRate, bi.is_key_material AS isKeyMaterial
                FROM bom_item bi
                JOIN material m ON m.material_id = bi.material_id
                WHERE bi.bom_id = ?
                ORDER BY bi.bom_item_id
                """, bomId);

        List<Map<String, Object>> routes = assignedRouteId == null
                ? jdbc.queryForList("""
                        SELECT pr.route_id, pr.route_code, pr.route_name, pr.version_no, pr.status,
                               pdr.is_default
                        FROM product_route pdr
                        JOIN process_route pr ON pr.route_id = pdr.route_id
                        WHERE pdr.product_id = ? AND pdr.status = 'ENABLED'
                          AND pr.status IN ('RELEASED', 'ENABLED')
                        ORDER BY pdr.is_default DESC, pdr.product_route_id DESC
                        LIMIT 1
                        """, productId)
                : jdbc.queryForList("""
                        SELECT route_id, route_code, route_name, version_no, status, 1 AS is_default
                        FROM process_route WHERE route_id = ?
                        """, assignedRouteId);
        Map<String, Object> route = routes.isEmpty() ? null : routes.get(0);
        Long routeId = route == null ? null : number(route.get("route_id"));
        List<Map<String, Object>> steps = routeId == null ? List.of() : jdbc.queryForList("""
                SELECT prs.step_seq AS stepSeq, ps.step_id AS stepId, ps.step_code AS stepCode,
                       ps.step_name AS stepName, ps.step_type AS stepType,
                       ps.standard_time_sec AS standardTimeSec, prs.station_type AS stationType,
                       prs.is_must_pass AS mustPass
                FROM process_route_step prs
                JOIN process_step ps ON ps.step_id = prs.step_id
                WHERE prs.route_id = ?
                ORDER BY prs.step_seq
                """, routeId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrderId", workOrderId);
        result.put("workOrderNo", workOrder.get("work_order_no"));
        result.put("productId", productId);
        result.put("productCode", workOrder.get("product_code"));
        result.put("productName", workOrder.get("product_name"));
        result.put("planQty", workOrder.get("plan_qty"));
        result.put("lifecycleStatus", workOrder.get("lifecycle_status"));
        result.put("bomReady", bom != null && !materials.isEmpty());
        result.put("bom", bom == null ? Map.of() : bom);
        result.put("materials", materials);
        result.put("routeReady", route != null && !steps.isEmpty());
        result.put("route", route == null ? Map.of() : route);
        result.put("steps", steps);
        return snapshot(result);
    }

    public Map<String, Object> tasks(Long workOrderId) {
        return snapshot(Map.of("workOrderId", workOrderId, "tasks", taskService.listByWorkOrder(workOrderId)));
    }

    public Map<String, Object> dispatches(Long workOrderId, Long operatorId) {
        String operatorFilter = operatorId == null ? "" : " AND dt.operator_id = ? ";
        List<Object> params = new ArrayList<>();
        params.add(workOrderId);
        if (operatorId != null) {
            params.add(operatorId);
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT dt.dispatch_id AS dispatchId, dt.dispatch_no AS dispatchNo,
                       dt.task_id AS taskId, dt.work_order_id AS workOrderId,
                       dt.planned_qty AS plannedQty, dt.completed_qty AS completedQty, dt.status,
                       dt.planned_start_time AS plannedStartTime, dt.planned_end_time AS plannedEndTime,
                       dt.actual_start_time AS actualStartTime, dt.actual_end_time AS actualEndTime,
                       ps.step_code AS processCode, ps.step_name AS processName,
                       ws.station_code AS workstationCode, ws.station_name AS workstationName,
                       u.username AS operatorName
                FROM dispatch_task dt
                LEFT JOIN process_step ps ON ps.step_id = dt.step_id
                LEFT JOIN workstation ws ON ws.station_id = dt.station_id
                LEFT JOIN sys_user u ON u.user_id = dt.operator_id
                WHERE dt.work_order_id = ?
                """ + operatorFilter + " ORDER BY COALESCE(dt.planned_start_time, dt.created_at), dt.dispatch_id", params.toArray());
        return snapshot(Map.of("workOrderId", workOrderId, "operatorScoped", operatorId != null, "dispatches", rows));
    }

    /** Uses the existing pure pipeline trace method; it does not call WorkOrderTraceSyncService. */
    public Map<String, Object> trace(String workOrderNo) {
        return snapshot(pipelineService.traceWorkOrder(workOrderNo));
    }

    private static Map<String, Object> snapshot(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>(facts);
        result.put("source", "MES_AGENT_READ_FACADE");
        result.put("queriedAt", Instant.now().toString());
        result.put("readOnly", true);
        return result;
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }
}
