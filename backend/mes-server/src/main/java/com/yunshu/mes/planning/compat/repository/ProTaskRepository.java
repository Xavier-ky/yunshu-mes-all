package com.yunshu.mes.planning.compat.repository;

import com.yunshu.mes.planning.compat.GanttMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ProTaskRepository {

    private static final String BASE = """
            SELECT pt.*, wo.work_order_no, wo.work_order_name, wo.product_id,
                   p.product_code, p.product_name,
                   ps.step_code AS process_code, ps.step_name AS process_name,
                   ws.station_code, ws.station_name,
                   pl.line_id AS pl_line_id, pl.line_name
            FROM production_task pt
            JOIN work_order wo ON pt.work_order_id = wo.work_order_id
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN process_step ps ON pt.step_id = ps.step_id
            LEFT JOIN workstation ws ON pt.workstation_id = ws.station_id
            LEFT JOIN production_line pl ON pt.line_id = pl.line_id
            WHERE 1=1
            """;

    private final JdbcTemplate jdbc;

    public ProTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query) {
        StringBuilder sql = new StringBuilder(BASE);
        List<Object> params = new ArrayList<>();
        filter(sql, params, query);
        sql.append(" ORDER BY pt.task_id DESC");
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), params.toArray());
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> list = jdbc.query(BASE + " AND pt.task_id = ?", (rs, n) -> mapRow(rs), id);
        return list.stream().findFirst();
    }

    public List<Map<String, Object>> findByWorkOrderId(Long workOrderId) {
        return jdbc.query(BASE + " AND pt.work_order_id = ? ORDER BY pt.start_time, pt.task_id",
                (rs, n) -> mapRow(rs), workOrderId);
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Long workstationId = longVal(body, "workstationId");
        Long lineId = longVal(body, "lineId");
        if (lineId == null && workstationId != null) {
            lineId = jdbc.query("""
                    SELECT line_id FROM workstation WHERE station_id = ?
                    """, rs -> rs.next() ? rs.getLong("line_id") : null, workstationId);
        }
        final Long resolvedLineId = lineId;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO production_task
                    (task_no, task_name, work_order_id, line_id, workstation_id, route_id, step_id,
                     duration_minutes, color_code, task_date, task_qty, completed_qty,
                     quantity_qualified, quantity_unqualified, status, start_time, end_time, request_date)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, strOr(body, "taskCode", "PT-" + System.currentTimeMillis()));
            ps.setString(i++, str(body, "taskName"));
            ps.setLong(i++, requireLong(body, "workorderId"));
            setLong(ps, i++, resolvedLineId);
            setLong(ps, i++, workstationId);
            setLong(ps, i++, longVal(body, "routeId"));
            setLong(ps, i++, longVal(body, "processId"));
            setInt(ps, i++, intVal(body, "duration"));
            ps.setString(i++, str(body, "colorCode"));
            ps.setDate(i++, java.sql.Date.valueOf(LocalDate.now()));
            ps.setBigDecimal(i++, decimal(body, "quantity", BigDecimal.ONE));
            ps.setBigDecimal(i++, decimal(body, "quantityProduced", BigDecimal.ZERO));
            ps.setBigDecimal(i++, decimal(body, "quantityQuanlify", BigDecimal.ZERO));
            ps.setBigDecimal(i++, decimal(body, "quantityUnquanlify", BigDecimal.ZERO));
            ps.setString(i++, strOr(body, "status", "NORMAL"));
            setTimestamp(ps, i++, parseTs(body.get("startTime")));
            setTimestamp(ps, i++, parseTs(body.get("endTime")));
            setTimestamp(ps, i++, parseTs(body.get("requestDate")));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long taskId = requireLong(body, "taskId");
        return jdbc.update("""
                UPDATE production_task SET
                  task_name = COALESCE(?, task_name),
                  workstation_id = COALESCE(?, workstation_id),
                  route_id = COALESCE(?, route_id),
                  step_id = COALESCE(?, step_id),
                  duration_minutes = COALESCE(?, duration_minutes),
                  color_code = COALESCE(?, color_code),
                  task_qty = COALESCE(?, task_qty),
                  quantity_qualified = COALESCE(?, quantity_qualified),
                  quantity_unqualified = COALESCE(?, quantity_unqualified),
                  status = COALESCE(?, status),
                  start_time = COALESCE(?, start_time),
                  end_time = COALESCE(?, end_time),
                  request_date = COALESCE(?, request_date)
                WHERE task_id = ?
                """,
                str(body, "taskName"), longVal(body, "workstationId"), longVal(body, "routeId"),
                longVal(body, "processId"), intVal(body, "duration"), str(body, "colorCode"),
                decimal(body, "quantity", null), decimal(body, "quantityQuanlify", null),
                decimal(body, "quantityUnquanlify", null), str(body, "status"),
                parseTs(body.get("startTime")), parseTs(body.get("endTime")), parseTs(body.get("requestDate")),
                taskId);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM production_task WHERE task_id = ?", id);
    }

    public void refreshScheduledQty(Long workOrderId) {
        jdbc.update("""
                UPDATE work_order wo SET quantity_scheduled = (
                  SELECT COALESCE(SUM(pt.task_qty), 0) FROM production_task pt WHERE pt.work_order_id = wo.work_order_id
                ) WHERE wo.work_order_id = ?
                """, workOrderId);
    }

    public List<Map<String, Object>> ganttTasks(Long workOrderId) {
        return listGanttTaskRows(workOrderId);
    }

    /** 已排产的生产任务 → 甘特 task 行。 */
    public List<Map<String, Object>> listGanttTaskRows(Long workOrderId) {
        String sql = BASE + " AND pt.work_order_id = ? ORDER BY pt.start_time, pt.task_id";
        return jdbc.query(sql, (rs, n) -> {
            BigDecimal qty = rs.getBigDecimal("task_qty");
            BigDecimal done = rs.getBigDecimal("completed_qty");
            String product = rs.getString("product_name");
            String unit = "PCS";
            String taskName = rs.getString("task_name");
            String process = rs.getString("process_name");
            if (taskName == null || taskName.isBlank()) {
                taskName = (process != null ? process : "生产任务")
                        + qty.stripTrailingZeros().toPlainString() + unit;
            }
            Timestamp start = rs.getTimestamp("start_time");
            if (start == null) {
                start = rs.getTimestamp("task_date") != null
                        ? new Timestamp(rs.getDate("task_date").getTime())
                        : new Timestamp(System.currentTimeMillis());
            }
            Integer dur = rs.getObject("duration_minutes") != null ? rs.getInt("duration_minutes") : null;
            return GanttMapper.toTaskRow(
                    String.valueOf(rs.getLong("task_id")),
                    workOrderId,
                    taskName,
                    product,
                    qty,
                    done,
                    process,
                    rs.getString("station_name"),
                    start,
                    dur,
                    rs.getString("color_code"));
        }, workOrderId);
    }

    /** 派工任务 → 甘特 task 行（种子数据工序级排程）。 */
    public List<Map<String, Object>> listGanttDispatchRows(Long workOrderId) {
        String sql = """
                SELECT dt.dispatch_id, dt.planned_qty, dt.completed_qty,
                       dt.planned_start_time, dt.actual_start_time, dt.actual_end_time,
                       ps.step_name AS process_name,
                       ws.station_name,
                       p.product_name,
                       pt.start_time AS task_start
                FROM dispatch_task dt
                JOIN work_order wo ON dt.work_order_id = wo.work_order_id
                LEFT JOIN product p ON wo.product_id = p.product_id
                LEFT JOIN process_step ps ON dt.step_id = ps.step_id
                LEFT JOIN workstation ws ON dt.station_id = ws.station_id
                LEFT JOIN production_task pt ON dt.task_id = pt.task_id
                WHERE dt.work_order_id = ?
                ORDER BY COALESCE(dt.actual_start_time, dt.planned_start_time, pt.start_time), dt.dispatch_id
                """;
        return jdbc.query(sql, (rs, n) -> {
            BigDecimal qty = rs.getBigDecimal("planned_qty");
            BigDecimal done = rs.getBigDecimal("completed_qty");
            String process = rs.getString("process_name");
            String station = rs.getString("station_name");
            String product = rs.getString("product_name");
            String text = (process != null ? process : "工序")
                    + " " + qty.stripTrailingZeros().toPlainString() + " PCS";
            Timestamp start = rs.getTimestamp("actual_start_time");
            if (start == null) {
                start = rs.getTimestamp("planned_start_time");
            }
            if (start == null) {
                start = rs.getTimestamp("task_start");
            }
            if (start == null) {
                start = new Timestamp(System.currentTimeMillis() + (long) n * 8 * 3600 * 1000);
            }
            Timestamp end = rs.getTimestamp("actual_end_time");
            Integer durationMinutes = null;
            if (end != null && start != null && end.after(start)) {
                durationMinutes = (int) ((end.getTime() - start.getTime()) / 60000);
            }
            return GanttMapper.toTaskRow(
                    "DT" + rs.getLong("dispatch_id"),
                    workOrderId,
                    text,
                    product,
                    qty,
                    done,
                    process,
                    station,
                    start,
                    durationMinutes,
                    null);
        }, workOrderId);
    }

    private void filter(StringBuilder sql, List<Object> params, Map<String, String> query) {
        if (StringUtils.hasText(query.get("workorderId"))) {
            sql.append(" AND pt.work_order_id = ?");
            params.add(Long.parseLong(query.get("workorderId")));
        }
        if (StringUtils.hasText(query.get("workstationId"))) {
            sql.append(" AND pt.workstation_id = ?");
            params.add(Long.parseLong(query.get("workstationId")));
        }
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskId", rs.getLong("task_id"));
        m.put("taskCode", rs.getString("task_no"));
        m.put("taskName", rs.getString("task_name"));
        m.put("workorderId", rs.getLong("work_order_id"));
        m.put("workorderCode", rs.getString("work_order_no"));
        m.put("workorderName", rs.getString("work_order_name"));
        m.put("productId", rs.getLong("product_id"));
        m.put("productCode", rs.getString("product_code"));
        m.put("productName", rs.getString("product_name"));
        m.put("lineId", rs.getObject("line_id"));
        m.put("lineName", rs.getString("line_name"));
        m.put("workstationId", rs.getObject("workstation_id"));
        m.put("workstationCode", rs.getString("station_code"));
        m.put("workstationName", rs.getString("station_name"));
        m.put("routeId", rs.getObject("route_id"));
        m.put("processId", rs.getObject("step_id"));
        m.put("processCode", rs.getString("process_code"));
        m.put("processName", rs.getString("process_name"));
        m.put("duration", rs.getObject("duration_minutes"));
        m.put("colorCode", rs.getString("color_code"));
        m.put("quantity", rs.getBigDecimal("task_qty"));
        m.put("quantityProduced", rs.getBigDecimal("completed_qty"));
        m.put("quantityQuanlify", rs.getBigDecimal("quantity_qualified"));
        m.put("quantityUnquanlify", rs.getBigDecimal("quantity_unqualified"));
        m.put("status", rs.getString("status"));
        m.put("startTime", rs.getTimestamp("start_time"));
        m.put("endTime", rs.getTimestamp("end_time"));
        m.put("requestDate", rs.getTimestamp("request_date"));
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Map<String, Object> body, String key, String def) {
        String s = str(body, key);
        return StringUtils.hasText(s) ? s : def;
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static long requireLong(Map<String, Object> body, String key) {
        Long v = longVal(body, key);
        if (v == null) {
            throw new IllegalArgumentException(key + " required");
        }
        return v;
    }

    private static Integer intVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    private static BigDecimal decimal(Map<String, Object> body, String key, BigDecimal def) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return def;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(v));
    }

    private static void setLong(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, v);
        }
    }

    private static void setInt(PreparedStatement ps, int idx, Integer v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, v);
        }
    }

    private static void setTimestamp(PreparedStatement ps, int idx, Timestamp ts) throws java.sql.SQLException {
        if (ts == null) {
            ps.setNull(idx, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(idx, ts);
        }
    }

    private static Timestamp parseTs(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        String s = String.valueOf(v);
        try {
            if (s.length() <= 10) {
                return Timestamp.valueOf(LocalDate.parse(s).atStartOfDay());
            }
            return Timestamp.valueOf(LocalDateTime.parse(s.replace(" ", "T")));
        } catch (Exception e) {
            return Timestamp.valueOf(LocalDate.parse(s.substring(0, 10)).atStartOfDay());
        }
    }
}
