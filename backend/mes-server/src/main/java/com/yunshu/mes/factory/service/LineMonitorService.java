package com.yunshu.mes.factory.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.factory.repository.ProductionLineRepository;
import com.yunshu.mes.factory.vo.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 产线监控聚合服务：优先 JDBC 实时查询，不可用时回退 Mock。
 */
@Service
public class LineMonitorService {

    private static final Logger log = LoggerFactory.getLogger(LineMonitorService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Known 3D positions aligned with frontend mock / seed data. */
    private static final Map<String, double[]> KNOWN_LINE_POSITIONS = Map.of(
            "LINE-FAN-01", new double[] { 300, 10, -1000 },
            "LINE-FAN-02", new double[] { 529, 10, -2500 },
            "LINE-FAN-03", new double[] { 300, 10, -4000 },
            "L-001", new double[] { 300, 10, -1000 },
            "L-002", new double[] { 529, 10, -2500 },
            "L-003", new double[] { 300, 10, -4000 }
    );

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ProductionLineRepository lineRepo;

    public LineMonitorService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                              ProductionLineRepository lineRepo) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.lineRepo = lineRepo;
    }

    public LineMonitorSummaryVO getSummary() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return fallbackSummary();
        }
        try {
            String refreshedAt = LocalDateTime.now().format(TIME_FMT);
            String shiftName = queryCurrentShift(jdbc);
            List<LineCardVO> lines = buildLineCards(jdbc);
            List<EquipmentStatusSliceVO> equipmentStatus = queryEquipmentStatus(jdbc);
            EquipmentSummaryVO equipmentSummary = buildEquipmentSummary(jdbc, equipmentStatus);
            List<LineMonitorMetricVO> metrics = buildSummaryMetrics(jdbc, lines, equipmentSummary.faultCount());
            List<LineMonitorAlertVO> alerts = queryOpenAndons(jdbc, null);
            AlertSummaryVO alertSummary = buildAlertSummary(alerts);
            return new LineMonitorSummaryVO(metrics, lines, alerts, alertSummary, equipmentStatus, equipmentSummary, shiftName, refreshedAt);
        } catch (DataAccessException e) {
            log.warn("产线监控总览回退 Mock：{}", e.getMessage());
            return fallbackSummary();
        }
    }

    public LineMonitorDetailVO getDetail(Long lineId) {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return fallbackDetail(lineId);
        }
        try {
            LineVO line = lineRepo.findById(lineId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产线不存在"));
            String refreshedAt = LocalDateTime.now().format(TIME_FMT);
            String shiftName = queryCurrentShift(jdbc);
            List<StationStatusVO> stations = buildStationStatuses(jdbc, lineId);
            List<WorkOrderSliceVO> workOrders = queryWorkOrdersForLine(jdbc, lineId);
            List<DispatchSliceVO> dispatches = queryDispatchesForLine(jdbc, lineId);
            List<AndonSliceVO> andons = queryAndonSlices(jdbc, lineId);
            List<HourPointVO> hourlyOutput = queryHourlyOutput(jdbc, lineId);
            OeeSliceVO oee = queryOeeForLine(jdbc, lineId, dispatches);
            List<EventLogVO> events = queryEvents(jdbc, lineId);
            return new LineMonitorDetailVO(line, stations, workOrders, dispatches, andons,
                    hourlyOutput, oee, events, shiftName, refreshedAt);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.warn("产线监控详情回退 Mock：{}", e.getMessage());
            return fallbackDetail(lineId);
        }
    }

    // ==================== JDBC queries ====================

    private String queryCurrentShift(JdbcTemplate jdbc) {
        try {
            List<String> names = jdbc.query("""
                    SELECT shift_name FROM factory_shift
                    WHERE status = 'ENABLED'
                    ORDER BY shift_id LIMIT 1
                    """, (rs, n) -> rs.getString("shift_name"));
            return names.isEmpty() ? "白班" : names.get(0);
        } catch (DataAccessException e) {
            return "白班";
        }
    }

    private List<LineCardVO> buildLineCards(JdbcTemplate jdbc) {
        List<LineVO> lines = lineRepo.findAll();
        List<LineCardVO> cards = new ArrayList<>();
        int slot = 0;
        for (LineVO line : lines) {
            TaskSlice task = queryActiveTask(jdbc, line.lineId());
            List<StationStatusVO> stations = buildStationStatuses(jdbc, line.lineId());
            StationStatusRatioVO ratio = toRatio(stations);
            int openAndons = countOpenAndonsForLine(jdbc, line.lineId());
            OeeSliceVO oee = queryOeeForLine(jdbc, line.lineId(), List.of());
            double plan = task != null ? task.planQty() : 0;
            double completed = task != null ? task.completedQty() : 0;
            double progress = plan > 0 ? Math.min(100, completed / plan * 100) : 0;
            String lineStatus = deriveLineStatus(ratio);
            Double[] modelPos = resolveModelPosition(line, slot++);
            cards.add(new LineCardVO(
                    line.lineId(), line.lineCode(), line.lineName(), line.workshopName(),
                    lineStatus,
                    task != null ? task.workOrderNo() : "—",
                    task != null ? task.productName() : "—",
                    plan, completed, progress,
                    oee.oee(), oee.estimated(),
                    ratio, openAndons,
                    modelPos[0], modelPos[1], modelPos[2]));
        }
        return cards;
    }

    /**
     * Resolve 3D model position: DB value first, then known lineCode map, then deterministic layout.
     */
    private Double[] resolveModelPosition(LineVO line, int slotIndex) {
        if (line.modelPosX() != null && line.modelPosZ() != null) {
            return new Double[] { line.modelPosX(), line.modelPosY(), line.modelPosZ() };
        }
        double[] known = KNOWN_LINE_POSITIONS.get(line.lineCode());
        if (known != null) {
            return new Double[] { known[0], known[1], known[2] };
        }
        long id = line.lineId() != null ? line.lineId() : slotIndex;
        int slot = (int) (Math.floorMod(id, 5));
        double x = 300 + slot * 115.0;
        double z = -1000 - slot * 1500.0;
        return new Double[] { x, 10.0, z };
    }

    private List<LineMonitorMetricVO> buildSummaryMetrics(JdbcTemplate jdbc, List<LineCardVO> lines, long faultDeviceCount) {
        int running = (int) lines.stream().filter(l -> "RUNNING".equals(l.lineStatus())).count();
        int fault = (int) lines.stream().filter(l -> "FAULT".equals(l.lineStatus())).count();
        int idle = lines.size() - running - fault;

        long activeWo = queryCount(jdbc, """
                SELECT COUNT(DISTINCT wo.work_order_id) FROM work_order wo
                JOIN production_task pt ON pt.work_order_id = wo.work_order_id
                WHERE wo.status IN ('RUNNING','DISPATCHED','CREATED')
                  AND wo.is_deleted = 0
                  AND pt.status IN ('RUNNING','DISPATCHED','CREATED')
                """);
        double todayOutput = queryDouble(jdbc, """
                SELECT COALESCE(SUM(pf.quantity_qualified), 0) FROM pro_feedback pf
                WHERE pf.status = 'FINISHED' AND DATE(pf.feedback_time) = CURDATE()
                """);
        long openAndons = queryCount(jdbc, """
                SELECT COUNT(*) FROM andon_event WHERE status = 'OPEN'
                """);
        long faultDevices = faultDeviceCount;
        double avgOee = lines.isEmpty() ? 0
                : lines.stream().mapToDouble(LineCardVO::oee).average().orElse(0);

        return List.of(
                new LineMonitorMetricVO("running_lines", "运行产线", String.valueOf(running), "条", "NORMAL"),
                new LineMonitorMetricVO("idle_lines", "待机产线", String.valueOf(idle), "条", "NORMAL"),
                new LineMonitorMetricVO("fault_lines", "故障产线", String.valueOf(fault), "条",
                        fault > 0 ? "ALERT" : "NORMAL"),
                new LineMonitorMetricVO("active_wo", "在制工单", String.valueOf(activeWo), "单", "NORMAL"),
                new LineMonitorMetricVO("today_output", "今日产出", String.format("%.0f", todayOutput), "台", "NORMAL"),
                new LineMonitorMetricVO("avg_oee", "平均OEE", String.format("%.1f", avgOee), "%", "NORMAL"),
                new LineMonitorMetricVO("open_andons", "待处理安灯", String.valueOf(openAndons), "件",
                        openAndons > 0 ? "WARNING" : "NORMAL"),
                new LineMonitorMetricVO("fault_devices", "故障设备", String.valueOf(faultDevices), "台",
                        faultDevices > 0 ? "ALERT" : "NORMAL")
        );
    }

    private List<LineMonitorAlertVO> queryOpenAndons(JdbcTemplate jdbc, Long lineId) {
        String sql = """
                SELECT ae.andon_id, ae.andon_no, ae.line_id,
                       COALESCE(l.line_name, '—') AS line_name,
                       CONCAT(COALESCE(at.type_name, '安灯'), '：', COALESCE(ae.exception_desc, '—')) AS message,
                       ae.status, DATE_FORMAT(ae.occur_time, '%m-%d %H:%i') AS occur_time,
                       COALESCE(at.type_name, '安灯') AS type_name,
                       COALESCE(at.type_code, 'UNKNOWN') AS type_code,
                       COALESCE(at.priority, 'NORMAL') AS priority,
                       COALESCE(ae.exception_desc, '—') AS exception_desc,
                       GREATEST(0, TIMESTAMPDIFF(MINUTE, ae.occur_time, NOW())) AS duration_minutes
                FROM andon_event ae
                LEFT JOIN production_line l ON ae.line_id = l.line_id
                LEFT JOIN andon_type at ON ae.andon_type_id = at.andon_type_id
                WHERE ae.status = 'OPEN'
                """ + (lineId != null ? " AND ae.line_id = ?" : "") + """
                ORDER BY ae.occur_time DESC LIMIT 20
                """;
        Object[] args = lineId != null ? new Object[]{lineId} : new Object[]{};
        return jdbc.query(sql, (rs, n) -> new LineMonitorAlertVO(
                rs.getLong("andon_id"),
                rs.getString("andon_no"),
                rs.getObject("line_id") == null ? null : rs.getLong("line_id"),
                rs.getString("line_name"),
                rs.getString("message"),
                rs.getString("status"),
                rs.getString("occur_time"),
                rs.getString("type_name"),
                rs.getString("type_code"),
                rs.getString("priority"),
                rs.getString("exception_desc"),
                rs.getLong("duration_minutes")), args);
    }

    private AlertSummaryVO buildAlertSummary(List<LineMonitorAlertVO> alerts) {
        if (alerts.isEmpty()) {
            return new AlertSummaryVO(0, 0, 0, 0, List.of());
        }
        long highPriority = alerts.stream()
                .filter(a -> "HIGH".equalsIgnoreCase(a.priority()))
                .count();
        long lineCount = alerts.stream()
                .map(LineMonitorAlertVO::lineId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long maxDuration = alerts.stream()
                .mapToLong(LineMonitorAlertVO::durationMinutes)
                .max()
                .orElse(0);
        Map<String, AlertTypeSliceVO> typeMap = new java.util.LinkedHashMap<>();
        for (LineMonitorAlertVO alert : alerts) {
            String code = alert.typeCode() == null ? "UNKNOWN" : alert.typeCode();
            String name = alert.typeName() == null ? "安灯" : alert.typeName();
            typeMap.merge(code, new AlertTypeSliceVO(code, name, 1),
                    (prev, cur) -> new AlertTypeSliceVO(code, name, prev.count() + 1));
        }
        List<AlertTypeSliceVO> typeSlices = typeMap.values().stream()
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
        return new AlertSummaryVO(alerts.size(), highPriority, lineCount, maxDuration, typeSlices);
    }

    private List<EquipmentStatusSliceVO> queryEquipmentStatus(JdbcTemplate jdbc) {
        try {
            return jdbc.query("""
                    SELECT UPPER(COALESCE(status, 'UNKNOWN')) AS status, COUNT(*) AS count
                    FROM dv_machinery
                    GROUP BY status ORDER BY count DESC
                    """, (rs, n) -> new EquipmentStatusSliceVO(
                    rs.getString("status"),
                    rs.getLong("count")));
        } catch (DataAccessException e) {
            log.warn("设备状态分布查询失败：{}", e.getMessage());
            return List.of();
        }
    }

    private EquipmentSummaryVO buildEquipmentSummary(JdbcTemplate jdbc, List<EquipmentStatusSliceVO> slices) {
        long total = 0;
        long runningCount = 0;
        long faultCount = 0;
        for (EquipmentStatusSliceVO slice : slices) {
            total += slice.count();
            String status = slice.status() == null ? "UNKNOWN" : slice.status().toUpperCase();
            if (isRunningEquipmentStatus(status)) {
                runningCount += slice.count();
            }
            if (isFaultEquipmentStatus(status)) {
                faultCount += slice.count();
            }
        }
        double runningRate = total > 0
                ? Math.round(runningCount * 1000.0 / total) / 10.0
                : 0;
        double avgOee = queryEquipmentAvgOee(jdbc);
        return new EquipmentSummaryVO(total, runningCount, runningRate, faultCount, avgOee);
    }

    private boolean isRunningEquipmentStatus(String status) {
        return "WORKING".equals(status) || "NORMAL".equals(status) || "RUNNING".equals(status);
    }

    private boolean isFaultEquipmentStatus(String status) {
        return "REPAIR".equals(status) || "STOP".equals(status) || "FAULT".equals(status)
                || "OFFLINE".equals(status);
    }

    private double queryEquipmentAvgOee(JdbcTemplate jdbc) {
        try {
            Double v = jdbc.queryForObject("""
                    SELECT ROUND(
                      SUM(CASE WHEN status = 'WORKING' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 1
                    ) FROM dv_machinery
                    """, Double.class);
            return v == null ? 0 : v;
        } catch (DataAccessException ignored) {
            return 0;
        }
    }

    private EquipmentSummaryVO buildEquipmentSummaryFromSlices(List<EquipmentStatusSliceVO> slices, double avgOee) {
        long total = 0;
        long runningCount = 0;
        long faultCount = 0;
        for (EquipmentStatusSliceVO slice : slices) {
            total += slice.count();
            String status = slice.status() == null ? "UNKNOWN" : slice.status().toUpperCase();
            if (isRunningEquipmentStatus(status)) {
                runningCount += slice.count();
            }
            if (isFaultEquipmentStatus(status)) {
                faultCount += slice.count();
            }
        }
        double runningRate = total > 0
                ? Math.round(runningCount * 1000.0 / total) / 10.0
                : 0;
        return new EquipmentSummaryVO(total, runningCount, runningRate, faultCount, avgOee);
    }

    private int countOpenAndonsForLine(JdbcTemplate jdbc, Long lineId) {
        return (int) queryCount(jdbc, "SELECT COUNT(*) FROM andon_event WHERE status = 'OPEN' AND line_id = ?", lineId);
    }

    private TaskSlice queryActiveTask(JdbcTemplate jdbc, Long lineId) {
        List<TaskSlice> list = jdbc.query("""
                SELECT wo.work_order_no, p.product_name, pt.task_qty, pt.completed_qty
                FROM production_task pt
                JOIN work_order wo ON pt.work_order_id = wo.work_order_id
                JOIN product p ON wo.product_id = p.product_id
                WHERE pt.line_id = ? AND pt.status IN ('RUNNING','DISPATCHED','CREATED')
                ORDER BY pt.updated_at DESC LIMIT 1
                """, (rs, n) -> new TaskSlice(
                rs.getString("work_order_no"),
                rs.getString("product_name"),
                rs.getDouble("task_qty"),
                rs.getDouble("completed_qty")), lineId);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<StationStatusVO> buildStationStatuses(JdbcTemplate jdbc, Long lineId) {
        List<StationRow> rows = jdbc.query("""
                SELECT s.station_id, s.station_code, s.station_name,
                       (SELECT m.machinery_name FROM md_workstation_machine wsm
                        JOIN dv_machinery m ON m.machinery_id = wsm.machinery_id
                        WHERE wsm.workstation_id = s.station_id
                          AND m.status IN ('REPAIR','STOP')
                        ORDER BY m.machinery_id LIMIT 1) AS device_name,
                       CASE
                         WHEN EXISTS (
                           SELECT 1 FROM md_workstation_machine wsm
                           JOIN dv_machinery m ON m.machinery_id = wsm.machinery_id
                           WHERE wsm.workstation_id = s.station_id
                             AND m.status IN ('REPAIR','STOP')
                         ) THEN 'FAULT'
                         WHEN EXISTS (
                           SELECT 1 FROM md_workstation_machine wsm
                           JOIN dv_machinery m ON m.machinery_id = wsm.machinery_id
                           WHERE wsm.workstation_id = s.station_id
                             AND m.status = 'WORKING'
                         ) THEN 'WORKING'
                         ELSE 'IDLE'
                       END AS device_status
                FROM workstation s
                WHERE s.line_id = ?
                ORDER BY s.station_code
                """, (rs, n) -> new StationRow(
                rs.getLong("station_id"),
                rs.getString("station_code"),
                rs.getString("station_name"),
                rs.getString("device_name"),
                rs.getString("device_status")), lineId);

        Map<Long, DispatchRow> dispatchByStation = new HashMap<>();
        List<DispatchStationRow> dispatchRows = jdbc.query("""
                SELECT dt.station_id, dt.dispatch_no, dt.planned_qty, dt.completed_qty, dt.status,
                       COALESCE(u.real_name, u.username, '—') AS operator_name
                FROM dispatch_task dt
                LEFT JOIN sys_user u ON dt.operator_id = u.user_id
                WHERE dt.station_id IN (
                    SELECT station_id FROM workstation WHERE line_id = ?
                ) AND dt.status IN ('RUNNING','DISPATCHED','CREATED','PAUSED')
                ORDER BY dt.updated_at DESC
                """, (rs, n) -> new DispatchStationRow(
                rs.getLong("station_id"),
                rs.getString("dispatch_no"),
                rs.getDouble("planned_qty"),
                rs.getDouble("completed_qty"),
                rs.getString("status"),
                rs.getString("operator_name")), lineId);
        for (DispatchStationRow dr : dispatchRows) {
            dispatchByStation.putIfAbsent(dr.stationId(), new DispatchRow(
                    dr.dispatchNo(), dr.plannedQty(), dr.completedQty(), dr.status(), dr.operatorName()));
        }

        Map<Long, Integer> andonByStation = new HashMap<>();
        List<AndonCountRow> andonCounts = jdbc.query("""
                SELECT station_id, COUNT(*) AS cnt FROM andon_event
                WHERE status = 'OPEN' AND station_id IN (
                    SELECT station_id FROM workstation WHERE line_id = ?
                ) GROUP BY station_id
                """, (rs, n) -> new AndonCountRow(rs.getLong("station_id"), rs.getInt("cnt")), lineId);
        for (AndonCountRow ac : andonCounts) {
            andonByStation.put(ac.stationId(), ac.count());
        }

        List<StationStatusVO> result = new ArrayList<>();
        for (StationRow row : rows) {
            DispatchRow dispatch = dispatchByStation.get(row.stationId());
            boolean deviceFault = "FAULT".equals(row.deviceStatus());
            boolean openAndon = andonByStation.getOrDefault(row.stationId(), 0) > 0;
            String dispatchStatus = dispatch != null ? dispatch.status() : null;
            boolean running = "RUNNING".equals(dispatchStatus)
                    || ("DISPATCHED".equals(dispatchStatus) && dispatch != null
                    && dispatch.completedQty() < dispatch.plannedQty());
            boolean dispatched = "DISPATCHED".equals(dispatchStatus) || "CREATED".equals(dispatchStatus);
            String runStatus = deriveStationStatus(deviceFault, openAndon, running, dispatched);
            result.add(new StationStatusVO(
                    row.stationId(), row.stationCode(), row.stationName(), runStatus,
                    dispatch != null ? dispatch.dispatchNo() : null,
                    dispatch != null ? dispatch.operatorName() : null,
                    dispatch != null ? dispatch.plannedQty() : 0,
                    dispatch != null ? dispatch.completedQty() : 0,
                    row.deviceName(), row.deviceStatus()));
        }
        return result;
    }

    private List<WorkOrderSliceVO> queryWorkOrdersForLine(JdbcTemplate jdbc, Long lineId) {
        return jdbc.query("""
                SELECT wo.work_order_id, wo.work_order_no, p.product_name,
                       wo.plan_qty, wo.completed_qty, wo.status
                FROM work_order wo
                JOIN production_task pt ON pt.work_order_id = wo.work_order_id
                JOIN product p ON wo.product_id = p.product_id
                WHERE pt.line_id = ? AND wo.status IN ('RUNNING','DISPATCHED','CREATED')
                GROUP BY wo.work_order_id, wo.work_order_no, p.product_name,
                         wo.plan_qty, wo.completed_qty, wo.status, wo.updated_at
                ORDER BY wo.updated_at DESC LIMIT 5
                """, (rs, n) -> new WorkOrderSliceVO(
                rs.getLong("work_order_id"),
                rs.getString("work_order_no"),
                rs.getString("product_name"),
                rs.getDouble("plan_qty"),
                rs.getDouble("completed_qty"),
                rs.getString("status")), lineId);
    }

    private List<DispatchSliceVO> queryDispatchesForLine(JdbcTemplate jdbc, Long lineId) {
        return jdbc.query("""
                SELECT dt.dispatch_id, dt.dispatch_no, st.station_name, ps.step_name,
                       COALESCE(u.real_name, u.username, '—') AS operator_name,
                       dt.planned_qty, dt.completed_qty, dt.status
                FROM dispatch_task dt
                JOIN production_task pt ON dt.task_id = pt.task_id
                LEFT JOIN workstation st ON dt.station_id = st.station_id
                LEFT JOIN process_step ps ON dt.step_id = ps.step_id
                LEFT JOIN sys_user u ON dt.operator_id = u.user_id
                WHERE pt.line_id = ? AND dt.status IN ('RUNNING','DISPATCHED','CREATED','PAUSED')
                ORDER BY dt.updated_at DESC LIMIT 10
                """, (rs, n) -> new DispatchSliceVO(
                rs.getLong("dispatch_id"),
                rs.getString("dispatch_no"),
                rs.getString("station_name"),
                rs.getString("step_name"),
                rs.getString("operator_name"),
                rs.getDouble("planned_qty"),
                rs.getDouble("completed_qty"),
                rs.getString("status")), lineId);
    }

    private List<AndonSliceVO> queryAndonSlices(JdbcTemplate jdbc, Long lineId) {
        return jdbc.query("""
                SELECT ae.andon_id, ae.andon_no, at.type_name,
                       COALESCE(ws.station_name, '—') AS station_name,
                       ae.exception_desc, ae.status,
                       DATE_FORMAT(ae.occur_time, '%m-%d %H:%i') AS occur_time
                FROM andon_event ae
                LEFT JOIN andon_type at ON ae.andon_type_id = at.andon_type_id
                LEFT JOIN workstation ws ON ae.station_id = ws.station_id
                WHERE ae.line_id = ? AND ae.status = 'OPEN'
                ORDER BY ae.occur_time DESC LIMIT 10
                """, (rs, n) -> new AndonSliceVO(
                rs.getLong("andon_id"),
                rs.getString("andon_no"),
                rs.getString("type_name"),
                rs.getString("station_name"),
                rs.getString("exception_desc"),
                rs.getString("status"),
                rs.getString("occur_time")), lineId);
    }

    private List<HourPointVO> queryHourlyOutput(JdbcTemplate jdbc, Long lineId) {
        Map<Integer, Double> hourMap = new HashMap<>();
        List<HourPointVO> raw = jdbc.query("""
                SELECT HOUR(pr.report_time) AS hr, COALESCE(SUM(pr.good_qty), 0) AS qty
                FROM production_report pr
                JOIN dispatch_task dt ON pr.dispatch_id = dt.dispatch_id
                JOIN production_task pt ON dt.task_id = pt.task_id
                WHERE pt.line_id = ? AND DATE(pr.report_time) = CURDATE()
                GROUP BY HOUR(pr.report_time)
                """, (rs, n) -> new HourPointVO(rs.getInt("hr"), rs.getDouble("qty")), lineId);
        for (HourPointVO p : raw) {
            hourMap.put(p.hour(), p.qty());
        }
        List<HourPointVO> points = new ArrayList<>();
        for (int h = 8; h <= 17; h++) {
            points.add(new HourPointVO(h, hourMap.getOrDefault(h, 0.0)));
        }
        return points;
    }

    private OeeSliceVO queryOeeForLine(JdbcTemplate jdbc, Long lineId, List<DispatchSliceVO> dispatches) {
        return estimateOee(dispatches);
    }

    private List<EventLogVO> queryEvents(JdbcTemplate jdbc, Long lineId) {
        return jdbc.query("""
                SELECT event_type, message, event_time FROM (
                    SELECT 'TASK' AS event_type,
                           CONCAT(tol.operation_type, ' · ', pt.task_no) AS message,
                           tol.operation_time AS event_time
                    FROM task_operation_log tol
                    JOIN production_task pt ON tol.task_id = pt.task_id
                    WHERE pt.line_id = ?
                    UNION ALL
                    SELECT 'ANDON',
                           CONCAT(ae.andon_no, ' · ', COALESCE(ae.exception_desc, '')),
                           ae.occur_time
                    FROM andon_event ae WHERE ae.line_id = ?
                ) ev ORDER BY event_time DESC LIMIT 10
                """, (rs, n) -> new EventLogVO(
                rs.getString("event_type"),
                rs.getString("message"),
                rs.getString("event_time")), lineId, lineId);
    }

    // ==================== helpers ====================

    private static String deriveStationStatus(boolean deviceFault, boolean openAndon,
                                              boolean running, boolean dispatched) {
        if (deviceFault) return "FAULT";
        if (openAndon) return "WARNING";
        if (running) return "RUNNING";
        if (dispatched) return "CHANGEOVER";
        return "IDLE";
    }

    private static String deriveLineStatus(StationStatusRatioVO ratio) {
        if (ratio.fault() > 0) return "FAULT";
        if (ratio.running() > 0) return "RUNNING";
        if (ratio.warning() > 0) return "WARNING";
        return "IDLE";
    }

    private static StationStatusRatioVO toRatio(List<StationStatusVO> stations) {
        int running = 0, warning = 0, fault = 0, changeover = 0, idle = 0;
        for (StationStatusVO s : stations) {
            switch (s.runStatus()) {
                case "RUNNING" -> running++;
                case "WARNING" -> warning++;
                case "FAULT" -> fault++;
                case "CHANGEOVER" -> changeover++;
                default -> idle++;
            }
        }
        return new StationStatusRatioVO(running, warning, fault, changeover, idle);
    }

    private static OeeSliceVO estimateOee(List<DispatchSliceVO> dispatches) {
        if (dispatches.isEmpty()) {
            return new OeeSliceVO(85, 78, 96, 64, true);
        }
        double planned = dispatches.stream().mapToDouble(DispatchSliceVO::plannedQty).sum();
        double completed = dispatches.stream().mapToDouble(DispatchSliceVO::completedQty).sum();
        double perf = planned > 0 ? Math.min(100, completed / planned * 100) : 75;
        return new OeeSliceVO(88, perf, 97, perf * 0.88 * 0.97 / 100, true);
    }

    private static double pct(double rate) {
        if (rate <= 0) return 0;
        return rate <= 1 ? rate * 100 : rate;
    }

    private long queryCount(JdbcTemplate jdbc, String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    private double queryDouble(JdbcTemplate jdbc, String sql, Object... args) {
        Double v = jdbc.queryForObject(sql, Double.class, args);
        return v == null ? 0 : v;
    }

    // ==================== Mock fallback ====================

    private LineMonitorSummaryVO fallbackSummary() {
        String refreshedAt = LocalDateTime.now().format(TIME_FMT);
        List<LineCardVO> lines = List.of(
                mockLineCard(1L, "L-001", "台扇组装一线", "RUNNING", "WO-20260701", "40cm台扇A型",
                        500, 320, 64, 82.5, false,
                        new StationStatusRatioVO(2, 0, 0, 1, 1), 1,
                        300.0, 10.0, -1000.0),
                mockLineCard(2L, "L-002", "落地扇组装一线", "RUNNING", "WO-20260702", "40cm落地扇A型",
                        300, 180, 60, 76.3, true,
                        new StationStatusRatioVO(1, 1, 0, 0, 2), 0,
                        529.0, 10.0, -2500.0),
                mockLineCard(3L, "L-003", "风扇组装线 A-01", "WARNING", "WO-20260703", "40cm落地电风扇A型",
                        200, 95, 47.5, 71.0, true,
                        new StationStatusRatioVO(1, 1, 0, 1, 1), 2,
                        300.0, 10.0, -4000.0)
        );
        List<LineMonitorMetricVO> metrics = List.of(
                new LineMonitorMetricVO("running_lines", "运行产线", "2", "条", "NORMAL"),
                new LineMonitorMetricVO("idle_lines", "待机产线", "0", "条", "NORMAL"),
                new LineMonitorMetricVO("fault_lines", "故障产线", "0", "条", "NORMAL"),
                new LineMonitorMetricVO("active_wo", "在制工单", "3", "单", "NORMAL"),
                new LineMonitorMetricVO("today_output", "今日产出", "595", "台", "NORMAL"),
                new LineMonitorMetricVO("avg_oee", "平均OEE", "76.6", "%", "NORMAL"),
                new LineMonitorMetricVO("open_andons", "待处理安灯", "3", "件", "WARNING"),
                new LineMonitorMetricVO("fault_devices", "故障设备", "5", "台", "ALERT")
        );
        List<LineMonitorAlertVO> alerts = List.of(
                new LineMonitorAlertVO(1L, "AD-001", 1L, "台扇组装一线", "缺料安灯：M4螺丝库存不足", "OPEN", "07-09 09:15",
                        "缺料安灯", "MATERIAL_SHORTAGE", "HIGH", "M4螺丝库存不足", 25),
                new LineMonitorAlertVO(2L, "AD-002", 7L, "风扇组装线 A-01", "质量安灯：设备噪音异常", "OPEN", "07-09 10:42",
                        "质量异常", "QUALITY_ABNORMAL", "HIGH", "设备噪音异常", 8),
                new LineMonitorAlertVO(3L, "AD-003", 7L, "风扇组装线 A-01", "工艺安灯：扇叶动平衡", "OPEN", "07-09 11:05",
                        "工艺求助", "PROCESS_HELP", "NORMAL", "扇叶动平衡", 5)
        );
        AlertSummaryVO alertSummary = buildAlertSummary(alerts);
        List<EquipmentStatusSliceVO> equipmentStatus = List.of(
                new EquipmentStatusSliceVO("WORKING", 13),
                new EquipmentStatusSliceVO("STOP", 3),
                new EquipmentStatusSliceVO("REPAIR", 2),
                new EquipmentStatusSliceVO("NORMAL", 1)
        );
        EquipmentSummaryVO equipmentSummary = buildEquipmentSummaryFromSlices(equipmentStatus, 64.0);
        return new LineMonitorSummaryVO(metrics, lines, alerts, alertSummary, equipmentStatus, equipmentSummary, "白班", refreshedAt);
    }

    private LineMonitorDetailVO fallbackDetail(Long lineId) {
        LineVO line = switch (lineId.intValue()) {
            case 2 -> new LineVO(2L, "L-002", "落地扇组装一线", "电风扇总装车间", "600", "台/班", "ENABLED",
                    529.0, 10.0, -2500.0);
            case 3 -> new LineVO(3L, "L-003", "风扇组装线 A-01", "电风扇总装车间", "500", "台/班", "ENABLED",
                    300.0, 10.0, -4000.0);
            default -> new LineVO(1L, "L-001", "台扇组装一线", "电风扇总装车间", "800", "台/班", "ENABLED",
                    300.0, 10.0, -1000.0);
        };
        List<StationStatusVO> stations = List.of(
                new StationStatusVO(1L, "ST-001", "底座装配工位", "RUNNING", "DT-001", "张三", 125, 80, "螺丝机01", "NORMAL"),
                new StationStatusVO(2L, "ST-002", "电机安装工位", "RUNNING", "DT-002", "李四", 125, 72, "—", "NORMAL"),
                new StationStatusVO(3L, "ST-003", "扇叶装配工位", "CHANGEOVER", "DT-003", "王五", 125, 0, "—", "NORMAL"),
                new StationStatusVO(4L, "ST-004", "整机测试工位", "WARNING", "DT-004", "赵六", 125, 45, "老化测试台01", "NORMAL")
        );
        List<WorkOrderSliceVO> workOrders = List.of(
                new WorkOrderSliceVO(1L, "WO-20260701", "40cm台扇A型", 500, 320, "RUNNING")
        );
        List<DispatchSliceVO> dispatches = List.of(
                new DispatchSliceVO(1L, "DT-001", "底座装配工位", "电机装配", "张三", 125, 80, "RUNNING"),
                new DispatchSliceVO(2L, "DT-002", "电机安装工位", "扇叶安装", "李四", 125, 72, "RUNNING"),
                new DispatchSliceVO(3L, "DT-003", "扇叶装配工位", "老化测试", "王五", 125, 0, "DISPATCHED"),
                new DispatchSliceVO(4L, "DT-004", "整机测试工位", "包装入箱", "赵六", 125, 45, "RUNNING")
        );
        List<AndonSliceVO> andons = lineId == 1L
                ? List.of(new AndonSliceVO(1L, "AD-001", "缺料安灯", "底座装配工位", "M4螺丝库存不足", "OPEN", "07-09 09:15"))
                : List.of();
        List<HourPointVO> hourly = List.of(
                new HourPointVO(8, 12), new HourPointVO(9, 28), new HourPointVO(10, 45),
                new HourPointVO(11, 52), new HourPointVO(12, 38), new HourPointVO(13, 41),
                new HourPointVO(14, 55), new HourPointVO(15, 49), new HourPointVO(16, 0), new HourPointVO(17, 0)
        );
        OeeSliceVO oee = new OeeSliceVO(88.5, 79.2, 97.1, 68.0, lineId != 1L);
        List<EventLogVO> events = List.of(
                new EventLogVO("TASK", "START · PT-20260701", "2026-07-09 08:05:00"),
                new EventLogVO("ANDON", "AD-001 · M4螺丝库存不足", "2026-07-09 09:15:22"),
                new EventLogVO("TASK", "REPORT · PT-20260701", "2026-07-09 10:30:00")
        );
        return new LineMonitorDetailVO(line, stations, workOrders, dispatches, andons, hourly, oee, events,
                "白班", LocalDateTime.now().format(TIME_FMT));
    }

    private static LineCardVO mockLineCard(Long id, String code, String name, String status,
                                           String woNo, String product, double plan, double done,
                                           double progress, double oee, boolean estimated,
                                           StationStatusRatioVO ratio, int andons,
                                           Double modelPosX, Double modelPosY, Double modelPosZ) {
        return new LineCardVO(id, code, name, "电风扇总装车间", status, woNo, product,
                plan, done, progress, oee, estimated, ratio, andons,
                modelPosX, modelPosY, modelPosZ);
    }

    private record TaskSlice(String workOrderNo, String productName, double planQty, double completedQty) {}
    private record StationRow(Long stationId, String stationCode, String stationName,
                              String deviceName, String deviceStatus) {}
    private record AndonCountRow(Long stationId, int count) {}
    private record DispatchStationRow(Long stationId, String dispatchNo, double plannedQty, double completedQty,
                                    String status, String operatorName) {}
    private record DispatchRow(String dispatchNo, double plannedQty, double completedQty,
                               String status, String operatorName) {}
}
