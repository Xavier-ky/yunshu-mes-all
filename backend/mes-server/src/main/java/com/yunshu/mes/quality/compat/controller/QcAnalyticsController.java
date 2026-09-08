package com.yunshu.mes.quality.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 质量分析聚合：KPI、趋势、类型分布、不良 TOP。
 */
@RestController
@RequestMapping("/api/mes/qc/analytics")
public class QcAnalyticsController {

    private final JdbcTemplate jdbc;
    private final QcPendingController pendingController;

    public QcAnalyticsController(JdbcTemplate jdbc, QcPendingController pendingController) {
        this.jdbc = jdbc;
        this.pendingController = pendingController;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingCount", countPending());
        data.put("todayFinished", countTodayFinished());
        data.put("passRate", calcPassRate());
        data.put("defectBatchCount", countDefectRecords());
        data.put("pendingDisposition", countPendingDisposition());
        return MesApiResponse.ok(data);
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(defaultValue = "7") int days) {
        int d = Math.min(Math.max(days, 1), 30);
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            rows = jdbc.query("""
                    SELECT day_label AS dayLabel,
                           SUM(total_cnt) AS total,
                           SUM(pass_cnt) AS passCount
                    FROM (
                      SELECT DATE(COALESCE(inspect_date, create_time)) AS day_label,
                             1 AS total_cnt,
                             IF(check_result = 'ACCEPT', 1, 0) AS pass_cnt
                      FROM qc_iqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT DATE(COALESCE(inspect_date, create_time)),
                             1,
                             IF(check_result = 'ACCEPT', 1, 0)
                      FROM qc_ipqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT DATE(COALESCE(inspect_date, create_time)),
                             1,
                             IF(check_result = 'ACCEPT', 1, 0)
                      FROM qc_oqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT DATE(COALESCE(inspect_date, create_time)),
                             1,
                             IF(check_result = 'ACCEPT', 1, 0)
                      FROM qc_rqc WHERE status = 'FINISHED'
                    ) t
                    WHERE day_label >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                    GROUP BY day_label
                    ORDER BY day_label
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("dayLabel", rs.getString("dayLabel"));
                long total = rs.getLong("total");
                long pass = rs.getLong("passCount");
                m.put("total", total);
                m.put("passCount", pass);
                m.put("passRate", total <= 0 ? 0 : Math.round(pass * 1000.0 / total) / 10.0);
                return m;
            }, d);
        } catch (Exception ignored) {
        }
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/type-distribution")
    public Map<String, Object> typeDistribution() {
        List<Map<String, Object>> rows = new ArrayList<>();
        addTypeCount(rows, "IQC", countTable("qc_iqc"));
        addTypeCount(rows, "PQC", countTable("qc_ipqc"));
        addTypeCount(rows, "OQC", countTable("qc_oqc"));
        addTypeCount(rows, "RQC", countTable("qc_rqc"));
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/defect-top")
    public Map<String, Object> defectTop(@RequestParam(defaultValue = "10") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            rows = jdbc.query("""
                    SELECT defect_name AS defectName,
                           SUM(defect_quantity) AS cnt,
                           CASE
                             WHEN SUM(CASE WHEN UPPER(defect_level) = 'CR' THEN defect_quantity ELSE 0 END) > 0 THEN 'CR'
                             WHEN SUM(CASE WHEN UPPER(defect_level) = 'MAJ' THEN defect_quantity ELSE 0 END) > 0 THEN 'MAJ'
                             ELSE 'MIN'
                           END AS defectLevel
                    FROM qc_defect_record
                    GROUP BY defect_name
                    ORDER BY cnt DESC
                    LIMIT ?
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("defectName", rs.getString("defectName"));
                m.put("cnt", rs.getLong("cnt"));
                m.put("defectLevel", rs.getString("defectLevel"));
                return m;
            }, lim);
        } catch (Exception ignored) {
        }
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/type-volume")
    public Map<String, Object> typeVolume() {
        List<Map<String, Object>> rows = new ArrayList<>();
        addTypeVolume(rows, "IQC", "qc_iqc");
        addTypeVolume(rows, "PQC", "qc_ipqc");
        addTypeVolume(rows, "OQC", "qc_oqc");
        addTypeVolume(rows, "RQC", "qc_rqc");
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/type-stats")
    public Map<String, Object> typeStats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("IQC", buildTypeStat("IQC", "qc_iqc"));
        data.put("PQC", buildTypeStat("PQC", "qc_ipqc"));
        data.put("OQC", buildTypeStat("OQC", "qc_oqc"));
        data.put("RQC", buildTypeStat("RQC", "qc_rqc"));
        return MesApiResponse.ok(data);
    }

    @GetMapping("/defect-by-level")
    public Map<String, Object> defectByLevel(@RequestParam(defaultValue = "7") int days) {
        int d = Math.min(Math.max(days, 1), 30);
        List<Map<String, Object>> rows = new ArrayList<>();
        try {
            rows = jdbc.query("""
                    SELECT UPPER(COALESCE(defect_level, 'MIN')) AS level,
                           SUM(defect_quantity) AS cnt
                    FROM qc_defect_record
                    WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
                    GROUP BY UPPER(COALESCE(defect_level, 'MIN'))
                    ORDER BY FIELD(level, 'CR', 'MAJ', 'MIN')
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("level", rs.getString("level"));
                m.put("cnt", rs.getLong("cnt"));
                return m;
            }, d);
        } catch (Exception ignored) {
        }
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/activity-feed")
    public Map<String, Object> activityFeed(@RequestParam(defaultValue = "12") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = queryActivityUnion(lim);
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/recent-finished")
    public Map<String, Object> recentFinished(@RequestParam(defaultValue = "8") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = queryRecentFinishedUnion(lim);
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/pending-disposition")
    public Map<String, Object> pendingDisposition(@RequestParam(defaultValue = "8") int limit) {
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = queryDocUnion("""
                SELECT 'IQC' AS qcType, iqc_code AS docCode, item_name AS itemName,
                       check_result AS checkResult, status,
                       COALESCE(inspect_date, update_time, create_time) AS inspectDate
                FROM qc_iqc WHERE status IN ('PREPARE','CONFIRMED')
                UNION ALL
                SELECT 'PQC', ipqc_code, item_name, check_result, status,
                       COALESCE(inspect_date, update_time, create_time)
                FROM qc_ipqc WHERE status IN ('PREPARE','CONFIRMED')
                UNION ALL
                SELECT 'OQC', oqc_code, item_name, check_result, status,
                       COALESCE(inspect_date, update_time, create_time)
                FROM qc_oqc WHERE status IN ('PREPARE','CONFIRMED')
                UNION ALL
                SELECT 'RQC', rqc_code, item_name, check_result, status,
                       COALESCE(inspect_date, update_time, create_time)
                FROM qc_rqc WHERE status IN ('PREPARE','CONFIRMED')
                """, lim);
        return MesApiResponse.ok(rows);
    }

    private List<Map<String, Object>> queryRecentFinishedUnion(int limit) {
        try {
            return jdbc.query("""
                    SELECT qcType, docCode, itemName, checkResult, status, finishTime
                    FROM (
                      SELECT 'IQC' AS qcType, iqc_code AS docCode, item_name AS itemName,
                             check_result AS checkResult, status,
                             COALESCE(update_time, create_time) AS finishTime,
                             iqc_id AS sortId
                      FROM qc_iqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT 'PQC', ipqc_code, item_name, check_result, status,
                             COALESCE(update_time, create_time), ipqc_id
                      FROM qc_ipqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT 'OQC', oqc_code, item_name, check_result, status,
                             COALESCE(update_time, create_time), oqc_id
                      FROM qc_oqc WHERE status = 'FINISHED'
                      UNION ALL
                      SELECT 'RQC', rqc_code, item_name, check_result, status,
                             COALESCE(update_time, create_time), rqc_id
                      FROM qc_rqc WHERE status = 'FINISHED'
                    ) u
                    ORDER BY finishTime DESC, sortId DESC
                    LIMIT ?
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("qcType", rs.getString("qcType"));
                m.put("docCode", rs.getString("docCode"));
                m.put("itemName", rs.getString("itemName"));
                m.put("checkResult", rs.getString("checkResult"));
                m.put("status", rs.getString("status"));
                m.put("inspectDate", formatDateTime(rs.getObject("finishTime")));
                return m;
            }, limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> queryDocUnion(String unionSql, int limit) {
        try {
            return jdbc.query("""
                    SELECT qcType, docCode, itemName, checkResult, status, inspectDate
                    FROM (""" + unionSql + """
                    ) u
                    ORDER BY inspectDate DESC
                    LIMIT ?
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("qcType", rs.getString("qcType"));
                m.put("docCode", rs.getString("docCode"));
                m.put("itemName", rs.getString("itemName"));
                m.put("checkResult", rs.getString("checkResult"));
                m.put("status", rs.getString("status"));
                m.put("inspectDate", formatDateTime(rs.getObject("inspectDate")));
                return m;
            }, limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String formatDateTime(Object dt) {
        if (dt == null) {
            return "";
        }
        String ds = String.valueOf(dt).replace("T", " ");
        if (ds.length() > 16) {
            ds = ds.substring(0, 16);
        }
        return ds;
    }

    private List<Map<String, Object>> queryActivityUnion(int limit) {
        try {
            return jdbc.query("""
                    SELECT qcType, docCode, itemName, checkResult, status, eventTime, eventType
                    FROM (
                      SELECT 'IQC' AS qcType, iqc_code AS docCode, item_name AS itemName,
                             check_result AS checkResult, status,
                             COALESCE(inspect_date, update_time, create_time) AS eventTime,
                             IF(status IN ('PREPARE','CONFIRMED'), 'PENDING', 'FINISHED') AS eventType
                      FROM qc_iqc
                      WHERE status = 'FINISHED' OR status IN ('PREPARE','CONFIRMED')
                      UNION ALL
                      SELECT 'PQC', ipqc_code, item_name, check_result, status,
                             COALESCE(inspect_date, update_time, create_time),
                             IF(status IN ('PREPARE','CONFIRMED'), 'PENDING', 'FINISHED')
                      FROM qc_ipqc
                      WHERE status = 'FINISHED' OR status IN ('PREPARE','CONFIRMED')
                      UNION ALL
                      SELECT 'OQC', oqc_code, item_name, check_result, status,
                             COALESCE(inspect_date, update_time, create_time),
                             IF(status IN ('PREPARE','CONFIRMED'), 'PENDING', 'FINISHED')
                      FROM qc_oqc
                      WHERE status = 'FINISHED' OR status IN ('PREPARE','CONFIRMED')
                      UNION ALL
                      SELECT 'RQC', rqc_code, item_name, check_result, status,
                             COALESCE(inspect_date, update_time, create_time),
                             IF(status IN ('PREPARE','CONFIRMED'), 'PENDING', 'FINISHED')
                      FROM qc_rqc
                      WHERE status = 'FINISHED' OR status IN ('PREPARE','CONFIRMED')
                    ) u
                    ORDER BY eventTime DESC
                    LIMIT ?
                    """, (rs, n) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("qcType", rs.getString("qcType"));
                m.put("docCode", rs.getString("docCode"));
                m.put("itemName", rs.getString("itemName"));
                m.put("checkResult", rs.getString("checkResult"));
                m.put("status", rs.getString("status"));
                m.put("eventType", rs.getString("eventType"));
                Object dt = rs.getObject("eventTime");
                String ds = dt == null ? "" : String.valueOf(dt).replace("T", " ");
                if (ds.length() > 19) {
                    ds = ds.substring(0, 19);
                }
                m.put("eventTime", ds);
                return m;
            }, limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> buildTypeStat(String qcType, String table) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pending", countTypePending(qcType));
        m.put("finishedToday", sumLong("""
                SELECT COUNT(*) FROM %s WHERE status='FINISHED'
                  AND DATE(COALESCE(inspect_date, update_time, create_time)) = CURDATE()
                """.formatted(table)));
        long total = sumLong("""
                SELECT COUNT(*) FROM %s WHERE status='FINISHED' AND check_result IS NOT NULL
                """.formatted(table));
        long pass = sumLong("""
                SELECT COUNT(*) FROM %s WHERE status='FINISHED' AND check_result='ACCEPT'
                """.formatted(table));
        m.put("passRate", total <= 0 ? 0 : Math.round(pass * 1000.0 / total) / 10.0);
        return m;
    }

    private long countTypePending(String qcType) {
        try {
            Object table = pendingController.list(Map.of()).get("rows");
            if (table instanceof List<?> list) {
                return list.stream().filter(r -> r instanceof Map<?, ?> map
                        && qcType.equals(String.valueOf(((Map<?, ?>) map).get("qcType")))).count();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private void addTypeVolume(List<Map<String, Object>> rows, String type, String table) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("qcType", type);
        m.put("passCnt", sumLong("""
                SELECT COUNT(*) FROM %s WHERE status='FINISHED' AND check_result='ACCEPT'
                """.formatted(table)));
        m.put("rejectCnt", sumLong("""
                SELECT COUNT(*) FROM %s WHERE status='FINISHED' AND check_result='REJECT'
                """.formatted(table)));
        rows.add(m);
    }

    private void addTypeCount(List<Map<String, Object>> rows, String type, long cnt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("qcType", type);
        m.put("cnt", cnt);
        rows.add(m);
    }

    private long countPending() {
        try {
            Object table = pendingController.list(Map.of()).get("rows");
            if (table instanceof List<?> list) {
                return list.size();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private long countTodayFinished() {
        return sumLong("""
                SELECT COUNT(*) FROM (
                  SELECT iqc_id FROM qc_iqc WHERE status='FINISHED'
                    AND DATE(COALESCE(inspect_date, update_time, create_time)) = CURDATE()
                  UNION ALL
                  SELECT ipqc_id FROM qc_ipqc WHERE status='FINISHED'
                    AND DATE(COALESCE(inspect_date, update_time, create_time)) = CURDATE()
                  UNION ALL
                  SELECT oqc_id FROM qc_oqc WHERE status='FINISHED'
                    AND DATE(COALESCE(inspect_date, update_time, create_time)) = CURDATE()
                  UNION ALL
                  SELECT rqc_id FROM qc_rqc WHERE status='FINISHED'
                    AND DATE(COALESCE(inspect_date, update_time, create_time)) = CURDATE()
                ) t
                """);
    }

    private double calcPassRate() {
        long total = sumLong("""
                SELECT COUNT(*) FROM (
                  SELECT iqc_id FROM qc_iqc WHERE status='FINISHED' AND check_result IS NOT NULL
                  UNION ALL
                  SELECT ipqc_id FROM qc_ipqc WHERE status='FINISHED' AND check_result IS NOT NULL
                  UNION ALL
                  SELECT oqc_id FROM qc_oqc WHERE status='FINISHED' AND check_result IS NOT NULL
                  UNION ALL
                  SELECT rqc_id FROM qc_rqc WHERE status='FINISHED' AND check_result IS NOT NULL
                ) t
                """);
        if (total <= 0) {
            return 0;
        }
        long pass = sumLong("""
                SELECT COUNT(*) FROM (
                  SELECT iqc_id FROM qc_iqc WHERE status='FINISHED' AND check_result='ACCEPT'
                  UNION ALL
                  SELECT ipqc_id FROM qc_ipqc WHERE status='FINISHED' AND check_result='ACCEPT'
                  UNION ALL
                  SELECT oqc_id FROM qc_oqc WHERE status='FINISHED' AND check_result='ACCEPT'
                  UNION ALL
                  SELECT rqc_id FROM qc_rqc WHERE status='FINISHED' AND check_result='ACCEPT'
                ) t
                """);
        return Math.round(pass * 1000.0 / total) / 10.0;
    }

    private long countDefectRecords() {
        return sumLong("SELECT COUNT(*) FROM (SELECT DISTINCT qc_type, qc_id FROM qc_defect_record) t");
    }

    private long countPendingDisposition() {
        return sumLong("""
                SELECT COUNT(*) FROM (
                  SELECT iqc_id FROM qc_iqc WHERE status IN ('PREPARE','CONFIRMED')
                  UNION ALL
                  SELECT ipqc_id FROM qc_ipqc WHERE status IN ('PREPARE','CONFIRMED')
                  UNION ALL
                  SELECT oqc_id FROM qc_oqc WHERE status IN ('PREPARE','CONFIRMED')
                  UNION ALL
                  SELECT rqc_id FROM qc_rqc WHERE status IN ('PREPARE','CONFIRMED')
                ) t
                """);
    }

    private long countTable(String table) {
        return sumLong("SELECT COUNT(*) FROM " + table);
    }

    private long sumLong(String sql) {
        try {
            Long v = jdbc.queryForObject(sql, Long.class);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
}
