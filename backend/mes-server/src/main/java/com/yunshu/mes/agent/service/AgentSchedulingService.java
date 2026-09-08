package com.yunshu.mes.agent.service;

import com.yunshu.mes.planning.compat.service.ProTaskService;
import com.yunshu.mes.planning.workflow.WorkOrderLifecycleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The controlled write boundary for the autonomous scheduling Agent.
 *
 * <p>It intentionally owns the complete write sequence: validate -> reserve
 * materials -> plan route steps -> create production tasks -> sync dispatches
 * -> validate. No frontend defaults or LLM-generated SQL may enter this path.
 */
@Service
public class AgentSchedulingService {

    private final JdbcTemplate jdbc;
    private final ProTaskService taskService;
    private final WorkOrderLifecycleService lifecycle;

    public AgentSchedulingService(
            JdbcTemplate jdbc, ProTaskService taskService, WorkOrderLifecycleService lifecycle) {
        this.jdbc = jdbc;
        this.taskService = taskService;
        this.lifecycle = lifecycle;
    }

    @Transactional
    public Map<String, Object> execute(Long workOrderId, Long userId) {
        Map<String, Object> workOrder = loadWorkOrder(workOrderId);
        if (workOrder.get("lifecycle_status") == null) {
            lifecycle.advance(workOrderId, WorkOrderLifecycleService.RELEASED);
            workOrder.put("lifecycle_status", WorkOrderLifecycleService.RELEASED);
        }
        ensureEligible(workOrder);

        List<Map<String, Object>> existingTasks = jdbc.queryForList(
                "SELECT task_id FROM production_task WHERE work_order_id = ?", workOrderId);
        if (!existingTasks.isEmpty()) {
            return validationResult(workOrderId, true, List.of(), "Work order already has scheduling tasks");
        }

        Long productId = number(workOrder.get("product_id"));
        BigDecimal planQty = decimal(workOrder.get("plan_qty"));
        Map<String, Object> bom = resolveReleasedBom(productId, number(workOrder.get("bom_id")));
        Map<String, Object> route = resolveEnabledRoute(productId, number(workOrder.get("route_id")));
        List<Map<String, Object>> bomItems = loadBomItems(number(bom.get("bom_id")));
        List<Map<String, Object>> routeSteps = loadRouteSteps(number(route.get("route_id")));
        if (bomItems.isEmpty() || routeSteps.isEmpty()) {
            throw new IllegalArgumentException("Released BOM and enabled route steps are required before scheduling");
        }
        materializeWorkOrderBomSnapshot(workOrderId, planQty, number(bom.get("bom_id")), bomItems);

        List<Map<String, Object>> shortages = reserveMaterials(workOrderId, planQty, bomItems, userId);
        if (!shortages.isEmpty()) {
            throw new IllegalStateException("Insufficient qualified inventory; scheduling was not created");
        }
        lifecycle.advanceIfNullOrEarlier(workOrderId, WorkOrderLifecycleService.KITTING_OK);

        LocalDateTime cursor = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.HOURS);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int i = 0; i < routeSteps.size(); i++) {
            Map<String, Object> step = routeSteps.get(i);
            Map<String, Object> station = chooseWorkstation(String.valueOf(step.get("station_type")), cursor);
            LocalDateTime stationFreeAt = latestStationEnd(number(station.get("station_id")), cursor);
            LocalDateTime start = cursor.isAfter(stationFreeAt) ? cursor : stationFreeAt;
            int durationMinutes = durationMinutes(planQty, number(step.get("standard_time_sec")));
            LocalDateTime end = start.plusMinutes(durationMinutes);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("taskCode", "AGT-" + workOrderId + "-" + (i + 1));
            body.put("taskName", step.get("step_name"));
            body.put("workorderId", workOrderId);
            body.put("routeId", route.get("route_id"));
            body.put("processId", step.get("step_id"));
            body.put("processCode", step.get("step_code"));
            body.put("processName", step.get("step_name"));
            body.put("workstationId", station.get("station_id"));
            body.put("workstationCode", station.get("station_code"));
            body.put("workstationName", station.get("station_name"));
            body.put("lineId", station.get("line_id"));
            body.put("quantity", planQty);
            body.put("duration", durationMinutes);
            body.put("startTime", start.toString().replace('T', ' '));
            body.put("endTime", end.toString().replace('T', ' '));
            body.put("status", "NORMAL");
            body.put("colorCode", ganttColor(i));
            Long taskId = taskService.create(body);

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("task_id", taskId);
            task.put("step_code", step.get("step_code"));
            task.put("step_name", step.get("step_name"));
            task.put("workstation_id", station.get("station_id"));
            task.put("workstation_code", station.get("station_code"));
            task.put("start_time", start.toString());
            task.put("end_time", end.toString());
            task.put("duration_minutes", durationMinutes);
            tasks.add(task);
            cursor = end;
        }

        return validationResult(workOrderId, false, tasks, "Scheduling created and dispatches synchronized");
    }

    /** Read-only advisory derived from live device, maintenance and staffing data. */
    public Map<String, Object> advisory(Long workOrderId) {
        List<Map<String, Object>> planned = jdbc.queryForList("""
                SELECT pt.workstation_id, ws.station_code, ws.station_name, pt.start_time, pt.end_time
                FROM production_task pt JOIN workstation ws ON ws.station_id = pt.workstation_id
                WHERE pt.work_order_id = ? ORDER BY pt.start_time, pt.task_id
                """, workOrderId);
        boolean deviceDataAvailable = tableExists("device") && tableExists("maintenance_task");
        List<Map<String, Object>> devices = deviceDataAvailable ? jdbc.queryForList("""
                SELECT d.device_id, d.device_code, d.device_name, d.status, ws.station_code
                FROM device d JOIN workstation ws ON ws.station_id = d.station_id
                WHERE d.station_id IN (SELECT workstation_id FROM production_task WHERE work_order_id = ?)
                ORDER BY d.device_id
                """, workOrderId) : List.of();
        List<Map<String, Object>> maintenance = deviceDataAvailable ? jdbc.queryForList("""
                SELECT mt.maintenance_task_id, mt.device_id, mt.planned_date, mt.status
                FROM maintenance_task mt
                WHERE mt.device_id IN (SELECT device_id FROM device WHERE station_id IN
                    (SELECT workstation_id FROM production_task WHERE work_order_id = ?))
                  AND mt.status NOT IN ('COMPLETED', 'CANCELLED')
                ORDER BY mt.planned_date
                """, workOrderId) : List.of();
        Long enabledOperators = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user su
                JOIN sys_user_role sur ON sur.user_id = su.user_id
                JOIN sys_role sr ON sr.role_id = sur.role_id
                WHERE su.status = 'ENABLED' AND sr.role_code = 'LINE_OPERATOR'
                """, Long.class);
        Long assignedDispatches = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dispatch_task WHERE work_order_id = ? AND operator_id IS NOT NULL
                """, Long.class, workOrderId);
        return Map.of(
                "informationalOnly", true,
                "deviceDataAvailable", deviceDataAvailable,
                "plannedStations", planned,
                "devices", devices,
                "nonNormalDeviceCount", devices.stream().filter(d -> !"NORMAL".equals(d.get("status"))).count(),
                "openMaintenance", maintenance,
                "enabledLineOperatorCount", enabledOperators == null ? 0 : enabledOperators,
                "assignedDispatchCount", assignedDispatches == null ? 0 : assignedDispatches);
    }

    private boolean tableExists(String tableName) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Long.class, tableName);
        return count != null && count > 0;
    }

    private Map<String, Object> loadWorkOrder(Long workOrderId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT work_order_id, work_order_no, product_id, bom_id, route_id, plan_qty,
                       status, lifecycle_status
                FROM work_order WHERE work_order_id = ? AND is_deleted = 0 FOR UPDATE
                """, workOrderId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Work order not found");
        return rows.get(0);
    }

    private static void ensureEligible(Map<String, Object> workOrder) {
        String status = String.valueOf(workOrder.get("status"));
        String lifecycleStatus = String.valueOf(workOrder.get("lifecycle_status"));
        if (!"CONFIRMED".equals(status) && !"DISPATCHED".equals(status)) {
            throw new IllegalStateException("Only confirmed work orders may enter autonomous scheduling");
        }
        if (!"RELEASED".equals(lifecycleStatus) && !"KITTING_OK".equals(lifecycleStatus)) {
            throw new IllegalStateException("Work order lifecycle is not eligible for scheduling: " + lifecycleStatus);
        }
    }

    private Map<String, Object> resolveReleasedBom(Long productId, Long assignedBomId) {
        String sql = assignedBomId == null
                ? "SELECT bom_id, bom_code, version_no FROM bom WHERE product_id = ? AND status IN ('RELEASED','ENABLED') ORDER BY bom_id DESC LIMIT 1"
                : "SELECT bom_id, bom_code, version_no FROM bom WHERE bom_id = ? AND status IN ('RELEASED','ENABLED')";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, assignedBomId == null ? productId : assignedBomId);
        if (rows.isEmpty()) throw new IllegalStateException("No released BOM is available");
        return rows.get(0);
    }

    private Map<String, Object> resolveEnabledRoute(Long productId, Long assignedRouteId) {
        String sql = assignedRouteId == null ? """
                SELECT pr.route_id, pr.route_code, pr.version_no
                FROM product_route pdr JOIN process_route pr ON pr.route_id = pdr.route_id
                WHERE pdr.product_id = ? AND pdr.status = 'ENABLED' AND pr.status IN ('RELEASED','ENABLED')
                ORDER BY pdr.is_default DESC, pdr.product_route_id DESC LIMIT 1
                """ : "SELECT route_id, route_code, version_no FROM process_route WHERE route_id = ? AND status IN ('RELEASED','ENABLED')";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, assignedRouteId == null ? productId : assignedRouteId);
        if (rows.isEmpty()) throw new IllegalStateException("No enabled production route is available");
        return rows.get(0);
    }

    private List<Map<String, Object>> loadBomItems(Long bomId) {
        return jdbc.queryForList("""
                SELECT bi.material_id, bi.qty_per, bi.loss_rate, bi.is_key_material,
                       m.material_code, m.material_name
                FROM bom_item bi JOIN material m ON m.material_id = bi.material_id
                WHERE bi.bom_id = ? ORDER BY bi.bom_item_id
                """, bomId);
    }

    private List<Map<String, Object>> loadRouteSteps(Long routeId) {
        return jdbc.queryForList("""
                SELECT prs.step_seq, prs.station_type, ps.step_id, ps.step_code, ps.step_name,
                       ps.standard_time_sec
                FROM process_route_step prs JOIN process_step ps ON ps.step_id = prs.step_id
                WHERE prs.route_id = ? ORDER BY prs.step_seq
                """, routeId);
    }

    /** Freeze the exact BOM selected by scheduling for all downstream WMS documents. */
    private void materializeWorkOrderBomSnapshot(
            Long workOrderId, BigDecimal planQty, Long bomId, List<Map<String, Object>> bomItems) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM work_order_bom WHERE work_order_id = ?", Long.class, workOrderId);
        if (count != null && count > 0) {
            return;
        }
        for (Map<String, Object> item : bomItems) {
            BigDecimal quantity = planQty.multiply(decimal(item.get("qty_per")))
                    .multiply(BigDecimal.ONE.add(decimal(item.get("loss_rate"))));
            jdbc.update("""
                    INSERT INTO work_order_bom
                    (work_order_id, material_id, material_code, material_name, unit_code, item_or_product, quantity, remark)
                    VALUES (?, ?, ?, ?, 'PCS', 'ITEM', ?, ?)
                    """,
                    workOrderId, item.get("material_id"), item.get("material_code"), item.get("material_name"),
                    quantity, "Frozen by AgentSchedulingService from BOM " + bomId);
        }
        jdbc.update("UPDATE work_order SET bom_id = COALESCE(bom_id, ?) WHERE work_order_id = ?", bomId, workOrderId);
    }

    private List<Map<String, Object>> reserveMaterials(
            Long workOrderId, BigDecimal planQty, List<Map<String, Object>> bomItems, Long userId) {
        List<Map<String, Object>> shortages = new ArrayList<>();
        for (Map<String, Object> item : bomItems) {
            Long materialId = number(item.get("material_id"));
            BigDecimal required = planQty.multiply(decimal(item.get("qty_per")))
                    .multiply(BigDecimal.ONE.add(decimal(item.get("loss_rate"))));
            List<Map<String, Object>> batches = jdbc.queryForList("""
                    SELECT batch_id, available_qty, locked_qty
                    FROM inventory_batch
                    WHERE material_id = ? AND status = 'IN_STOCK' AND quality_status = 'QUALIFIED'
                    ORDER BY batch_id FOR UPDATE
                    """, materialId);
            BigDecimal available = batches.stream()
                    .map(b -> decimal(b.get("available_qty")).subtract(decimal(b.get("locked_qty"))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(required) < 0) {
                shortages.add(Map.of(
                        "material_id", materialId,
                        "material_code", item.get("material_code"),
                        "required_qty", required,
                        "available_qty", available,
                        "shortage_qty", required.subtract(available)));
                continue;
            }
            BigDecimal remaining = required;
            for (Map<String, Object> batch : batches) {
                BigDecimal free = decimal(batch.get("available_qty")).subtract(decimal(batch.get("locked_qty")));
                BigDecimal lock = free.min(remaining);
                if (lock.signum() > 0) {
                    jdbc.update("UPDATE inventory_batch SET locked_qty = locked_qty + ? WHERE batch_id = ?", lock, batch.get("batch_id"));
                    remaining = remaining.subtract(lock);
                }
                if (remaining.signum() == 0) break;
            }
        }
        if (!shortages.isEmpty()) return shortages;
        jdbc.update("""
                INSERT INTO kitting_analysis (work_order_id, analysis_status, required_summary, available_summary, analyzed_by)
                VALUES (?, 'SUFFICIENT', JSON_OBJECT('source','AGENT_SCHEDULING'), JSON_OBJECT('reserved', true), ?)
                """, workOrderId, userId);
        jdbc.update("UPDATE work_order SET status = 'DISPATCHED' WHERE work_order_id = ?", workOrderId);
        return shortages;
    }

    private Map<String, Object> chooseWorkstation(String stationType, LocalDateTime from) {
        List<Map<String, Object>> stations = jdbc.queryForList("""
                SELECT ws.station_id, ws.station_code, ws.station_name, ws.line_id
                FROM workstation ws JOIN production_line pl ON pl.line_id = ws.line_id
                WHERE ws.status = 'ENABLED' AND pl.status = 'ENABLED'
                  AND (? IS NULL OR ? = '' OR ws.station_type = ?)
                ORDER BY ws.station_id
                """, stationType, stationType, stationType);
        if (stations.isEmpty()) {
            stations = jdbc.queryForList("""
                    SELECT ws.station_id, ws.station_code, ws.station_name, ws.line_id
                    FROM workstation ws JOIN production_line pl ON pl.line_id = ws.line_id
                    WHERE ws.status = 'ENABLED' AND pl.status = 'ENABLED' ORDER BY ws.station_id
                    """);
        }
        if (stations.isEmpty()) throw new IllegalStateException("No enabled workstation is available for scheduling");
        return stations.stream()
                .min(Comparator.comparing(station -> latestStationEnd(number(station.get("station_id")), from)))
                .orElseThrow();
    }

    private LocalDateTime latestStationEnd(Long stationId, LocalDateTime floor) {
        LocalDateTime latest = jdbc.query("""
                SELECT MAX(COALESCE(end_time, start_time)) FROM production_task
                WHERE workstation_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED')
                  AND COALESCE(end_time, start_time) > ?
                """, rs -> {
                    java.sql.Timestamp timestamp = rs.next() ? rs.getTimestamp(1) : null;
                    return timestamp == null ? null : timestamp.toLocalDateTime();
                }, stationId, java.sql.Timestamp.valueOf(floor));
        return latest == null ? floor : latest;
    }

    private Map<String, Object> validationResult(
            Long workOrderId, boolean alreadyScheduled, List<Map<String, Object>> createdTasks, String message) {
        Long taskCount = jdbc.queryForObject("SELECT COUNT(*) FROM production_task WHERE work_order_id = ?", Long.class, workOrderId);
        Long dispatchCount = jdbc.queryForObject("SELECT COUNT(*) FROM dispatch_task WHERE work_order_id = ?", Long.class, workOrderId);
        Map<String, Object> workOrder = jdbc.queryForMap("""
                SELECT work_order_no, status, lifecycle_status, quantity_scheduled
                FROM work_order WHERE work_order_id = ?
                """, workOrderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workOrderId", workOrderId);
        result.put("workOrderNo", workOrder.get("work_order_no"));
        result.put("alreadyScheduled", alreadyScheduled);
        result.put("createdTasks", createdTasks);
        result.put("productionTaskCount", taskCount == null ? 0 : taskCount);
        result.put("dispatchTaskCount", dispatchCount == null ? 0 : dispatchCount);
        result.put("workOrderStatus", workOrder.get("status"));
        result.put("lifecycleStatus", workOrder.get("lifecycle_status"));
        result.put("quantityScheduled", workOrder.get("quantity_scheduled"));
        result.put("message", message);
        return result;
    }

    private static int durationMinutes(BigDecimal qty, Long secondsPerPiece) {
        long seconds = secondsPerPiece == null || secondsPerPiece <= 0 ? 3600L : secondsPerPiece;
        BigDecimal minutes = qty.multiply(BigDecimal.valueOf(seconds)).divide(BigDecimal.valueOf(60), 0, RoundingMode.CEILING);
        return Math.max(15, minutes.min(BigDecimal.valueOf(24 * 60)).intValue());
    }

    private static String ganttColor(int index) {
        String[] colors = {"#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#9B59B6", "#1ABC9C"};
        return colors[index % colors.length];
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
