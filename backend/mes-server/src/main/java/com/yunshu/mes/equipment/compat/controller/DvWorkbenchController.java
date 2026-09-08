package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.service.EquipmentBridgeService;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/dv/workbench")
public class DvWorkbenchController {

    private static final String SQL_COUNT_CHECK_PREPARE =
            "SELECT COUNT(*) FROM dv_check_record WHERE status = 'PREPARE'";
    private static final String SQL_COUNT_MAINTEN_PREPARE =
            "SELECT COUNT(*) FROM dv_mainten_record WHERE status = 'PREPARE'";
    private static final String SQL_COUNT_REPAIR_PENDING =
            "SELECT COUNT(*) FROM dv_repair WHERE status IS NULL OR status IN ('PREPARE','CONFIRMED')";
    private static final String SQL_COUNT_FAULT_MACHINERY =
            "SELECT COUNT(*) FROM dv_machinery WHERE status IN ('REPAIR','STOP')";
    private static final String SQL_COUNT_MACHINERY =
            "SELECT COUNT(*) FROM dv_machinery";
    private static final String SQL_AVG_OEE =
            "SELECT ROUND(SUM(CASE WHEN status = 'WORKING' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 1) FROM dv_machinery";
    private static final String SQL_COUNT_TODAY_FINISHED =
            "SELECT COUNT(*) FROM ("
                    + "SELECT record_id FROM dv_check_record WHERE status = 'FINISHED' AND DATE(check_time) = CURDATE() "
                    + "UNION ALL SELECT record_id FROM dv_mainten_record WHERE status = 'FINISHED' AND DATE(mainten_time) = CURDATE() "
                    + "UNION ALL SELECT repair_id FROM dv_repair WHERE status = 'FINISHED' AND DATE(COALESCE(finish_date, confirm_date)) = CURDATE()"
                    + ") t";
    private static final String SQL_PENDING_CHECK =
            "SELECT record_id AS taskId, machinery_id AS machineryId, machinery_code AS machineryCode, "
                    + "machinery_name AS machineryName, plan_id AS planId, plan_code AS planCode, plan_name AS planName, "
                    + "status, check_time AS sortTime FROM dv_check_record WHERE status = 'PREPARE' "
                    + "ORDER BY check_time DESC LIMIT ?";
    private static final String SQL_PENDING_MAINTEN =
            "SELECT record_id AS taskId, machinery_id AS machineryId, machinery_code AS machineryCode, "
                    + "machinery_name AS machineryName, plan_id AS planId, plan_code AS planCode, plan_name AS planName, "
                    + "status, mainten_time AS sortTime FROM dv_mainten_record WHERE status = 'PREPARE' "
                    + "ORDER BY mainten_time DESC LIMIT ?";
    private static final String SQL_PENDING_REPAIR =
            "SELECT repair_id AS taskId, machinery_id AS machineryId, machinery_code AS machineryCode, "
                    + "machinery_name AS machineryName, repair_code AS planCode, repair_name AS planName, "
                    + "status, require_date AS sortTime FROM dv_repair "
                    + "WHERE status IS NULL OR status IN ('PREPARE','CONFIRMED') ORDER BY require_date DESC LIMIT ?";
    private static final String SQL_RECENT_FINISHED =
            "SELECT * FROM ("
                    + "SELECT 'CHECK' AS taskType, record_id AS taskId, machinery_code AS machineryCode, "
                    + "machinery_name AS machineryName, plan_code AS planCode, plan_name AS planName, "
                    + "status, DATE_FORMAT(check_time, '%Y-%m-%d %H:%i') AS finishTime, check_time AS sortTime "
                    + "FROM dv_check_record WHERE status = 'FINISHED' UNION ALL "
                    + "SELECT 'MAINTEN', record_id, machinery_code, machinery_name, plan_code, plan_name, "
                    + "status, DATE_FORMAT(mainten_time, '%Y-%m-%d %H:%i'), mainten_time "
                    + "FROM dv_mainten_record WHERE status = 'FINISHED' UNION ALL "
                    + "SELECT 'REPAIR', repair_id, machinery_code, machinery_name, repair_code, repair_name, "
                    + "status, DATE_FORMAT(COALESCE(finish_date, confirm_date, require_date), '%Y-%m-%d %H:%i'), "
                    + "COALESCE(finish_date, confirm_date, require_date) FROM dv_repair WHERE status = 'FINISHED'"
                    + ") t ORDER BY sortTime DESC LIMIT ?";
    private static final String SQL_STATUS_DISTRIBUTION =
            "SELECT status, COUNT(*) AS count FROM dv_machinery GROUP BY status ORDER BY count DESC";
    private static final String SQL_MAINTENANCE_TREND =
            "SELECT DATE_FORMAT(day_label, '%Y-%m-%d') AS dayLabel, SUM(cnt) AS total FROM ("
                    + "SELECT DATE(check_time) AS day_label, 1 AS cnt FROM dv_check_record "
                    + "WHERE check_time >= DATE_SUB(CURDATE(), INTERVAL 29 DAY) UNION ALL "
                    + "SELECT DATE(mainten_time), 1 FROM dv_mainten_record "
                    + "WHERE mainten_time >= DATE_SUB(CURDATE(), INTERVAL 29 DAY) UNION ALL "
                    + "SELECT DATE(require_date), 1 FROM dv_repair "
                    + "WHERE require_date >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)"
                    + ") t GROUP BY day_label ORDER BY day_label";

    private static final RowMapper<Map<String, Object>> RECENT_FINISHED_ROW_MAPPER = (rs, n) -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskType", rs.getString("taskType"));
        m.put("taskId", rs.getLong("taskId"));
        m.put("machineryCode", rs.getString("machineryCode"));
        m.put("machineryName", rs.getString("machineryName"));
        m.put("planCode", rs.getString("planCode"));
        m.put("planName", rs.getString("planName"));
        m.put("status", rs.getString("status"));
        m.put("finishTime", rs.getString("finishTime"));
        return m;
    };

    private static final Comparator<Map<String, Object>> PENDING_ROW_COMPARATOR = (a, b) -> {
        int rank = Integer.compare(pendingTypeRank(a.get("taskType")), pendingTypeRank(b.get("taskType")));
        if (rank != 0) {
            return rank;
        }
        return String.valueOf(b.get("sortTime")).compareTo(String.valueOf(a.get("sortTime")));
    };

    private final JdbcTemplate jdbc;
    private final EquipmentBridgeService equipmentBridge;

    public DvWorkbenchController(JdbcTemplate jdbc, EquipmentBridgeService equipmentBridge) {
        this.jdbc = jdbc;
        this.equipmentBridge = equipmentBridge;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingCheck", countSql(SQL_COUNT_CHECK_PREPARE));
        data.put("pendingMainten", countSql(SQL_COUNT_MAINTEN_PREPARE));
        data.put("pendingRepair", countSql(SQL_COUNT_REPAIR_PENDING));
        data.put("faultCount", countSql(SQL_COUNT_FAULT_MACHINERY));
        data.put("avgOee", calcAvgOee());
        data.put("machineryTotal", countSql(SQL_COUNT_MACHINERY));
        data.put("todayFinished", countSql(SQL_COUNT_TODAY_FINISHED));
        return MesApiResponse.ok(data);
    }

    @GetMapping("/pending")
    public Map<String, Object> pending(@RequestParam(defaultValue = "100") int limit) {
        int lim = Math.min(Math.max(limit, 1), 200);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(queryPendingTasks("CHECK", SQL_PENDING_CHECK, lim));
        rows.addAll(queryPendingTasks("MAINTEN", SQL_PENDING_MAINTEN, lim));
        rows.addAll(queryPendingTasks("REPAIR", SQL_PENDING_REPAIR, lim));
        rows.sort(PENDING_ROW_COMPARATOR);
        if (rows.size() > lim) {
            rows = rows.subList(0, lim);
        }
        rows.forEach(equipmentBridge::enrichPendingRow);
        return MesApiResponse.table(rows, rows.size());
    }

    @PostMapping("/repair-from-andon/{recordId}")
    public Map<String, Object> repairFromAndon(@PathVariable Long recordId) {
        Long repairId = equipmentBridge.createRepairFromAndonRecord(recordId, false);
        if (repairId == null) {
            return MesApiResponse.error("无法创建维修单：工位未绑定设备或安灯已关联维修单");
        }
        return MesApiResponse.ok(repairId);
    }

    @GetMapping("/station-context")
    public Map<String, Object> stationContext(@RequestParam Long machineryId) {
        return equipmentBridge.findStationContextByMachinery(machineryId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("未找到工位绑定"));
    }

    @GetMapping("/recent-finished")
    public Map<String, Object> recentFinished(@RequestParam(defaultValue = "8") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = jdbc.query(SQL_RECENT_FINISHED, RECENT_FINISHED_ROW_MAPPER, lim);
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/analytics-overview")
    public Map<String, Object> analyticsOverview() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("machineryTotal", countSql(SQL_COUNT_MACHINERY));
        summary.put("faultCount", countSql(SQL_COUNT_FAULT_MACHINERY));
        summary.put("pendingTotal",
                countSql(SQL_COUNT_CHECK_PREPARE)
                        + countSql(SQL_COUNT_MAINTEN_PREPARE)
                        + countSql(SQL_COUNT_REPAIR_PENDING));
        summary.put("avgOee", calcAvgOee());
        data.put("summary", summary);
        data.put("statusDistribution", jdbc.query(SQL_STATUS_DISTRIBUTION, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", rs.getString("status"));
            m.put("count", rs.getLong("count"));
            return m;
        }));
        data.put("maintenanceTrend", jdbc.query(SQL_MAINTENANCE_TREND, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("dayLabel", rs.getString("dayLabel"));
            m.put("total", rs.getLong("total"));
            return m;
        }));
        return MesApiResponse.ok(data);
    }

    private long countSql(String sql) {
        Long c = jdbc.queryForObject(sql, Long.class);
        return c == null ? 0 : c;
    }

    private double calcAvgOee() {
        try {
            Double v = jdbc.queryForObject(SQL_AVG_OEE, Double.class);
            return v == null ? 0 : v;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private List<Map<String, Object>> queryPendingTasks(String taskType, String sql, int limit) {
        return jdbc.query(sql, (rs, n) -> mapPendingRow(rs, taskType), limit);
    }

    private static Map<String, Object> mapPendingRow(ResultSet rs, String taskType) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskType", taskType);
        m.put("taskId", rs.getLong("taskId"));
        m.put("machineryId", rs.getObject("machineryId"));
        m.put("machineryCode", rs.getString("machineryCode"));
        m.put("machineryName", rs.getString("machineryName"));
        if ("REPAIR".equals(taskType)) {
            m.put("planId", null);
        } else {
            m.put("planId", rs.getObject("planId"));
        }
        m.put("planCode", rs.getString("planCode"));
        m.put("planName", rs.getString("planName"));
        m.put("status", rs.getString("status"));
        m.put("sortTime", rs.getTimestamp("sortTime"));
        return m;
    }

    /** 工作台左侧待办：维修优先，其次保养、点检 */
    private static int pendingTypeRank(Object taskType) {
        if (taskType == null) {
            return 9;
        }
        String type = String.valueOf(taskType);
        if ("REPAIR".equals(type)) {
            return 0;
        }
        if ("MAINTEN".equals(type)) {
            return 1;
        }
        if ("CHECK".equals(type)) {
            return 2;
        }
        return 9;
    }
}
