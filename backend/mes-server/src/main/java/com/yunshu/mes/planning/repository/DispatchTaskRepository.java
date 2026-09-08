package com.yunshu.mes.planning.repository;

import com.yunshu.mes.planning.vo.DispatchTaskVO;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 派工单数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class DispatchTaskRepository {

    private static final String SELECT_LIST = """
            SELECT dt.dispatch_id, dt.dispatch_no, pt.task_no, dt.task_id, dt.work_order_id, wo.work_order_no,
                   dt.step_id, s.step_code, s.step_name, dt.station_id, st.station_code, st.station_name,
                   p.product_id, p.product_code, p.product_name,
                   pl.line_name, pl.line_code,
                   dt.operator_id, u.username, dt.planned_qty, dt.completed_qty, dt.status,
                   dt.planned_start_time, dt.planned_end_time, dt.actual_start_time,
                   co.order_no AS customer_order_no, dt.created_at
            FROM dispatch_task dt
            LEFT JOIN production_task pt ON dt.task_id = pt.task_id
            LEFT JOIN work_order wo ON dt.work_order_id = wo.work_order_id
            LEFT JOIN customer_order co ON co.order_id = wo.order_id
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN production_line pl ON pt.line_id = pl.line_id
            LEFT JOIN process_step s ON dt.step_id = s.step_id
            LEFT JOIN workstation st ON dt.station_id = st.station_id
            LEFT JOIN sys_user u ON dt.operator_id = u.user_id
            ORDER BY dt.dispatch_id
            """;
    private static final String SELECT_BY_ID = SELECT_LIST.replace("ORDER BY dt.dispatch_id", "WHERE dt.dispatch_id = ?");

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public DispatchTaskRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<DispatchTaskVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, this::mapRow);
    }

    public Optional<DispatchTaskVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<DispatchTaskVO> list = jdbc.query(SELECT_BY_ID, this::mapRow, id);
        return list.stream().findFirst();
    }

    public Long insert(String dispatchNo, Long taskId, Long workOrderId, BigDecimal plannedQty, Long stepId, Long stationId, Long assigneeId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dispatch_task (dispatch_no, task_id, work_order_id, planned_qty, step_id, station_id, operator_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dispatchNo);
            ps.setLong(2, taskId);
            ps.setLong(3, workOrderId);
            ps.setBigDecimal(4, plannedQty != null ? plannedQty : BigDecimal.ZERO);
            if (stepId != null) {
                ps.setLong(5, stepId);
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            if (stationId != null) {
                ps.setLong(6, stationId);
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            if (assigneeId != null) {
                ps.setLong(7, assigneeId);
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            ps.setString(8, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String dispatchNo, Long taskId, Long workOrderId, BigDecimal plannedQty, Long stepId, Long stationId, Long assigneeId, String status) {
        JdbcTemplate jdbc = requireJdbc();
        if ("RUNNING".equalsIgnoreCase(status)) {
            jdbc.update("""
                    UPDATE dispatch_task SET dispatch_no = ?, task_id = ?, work_order_id = ?, planned_qty = ?,
                      step_id = ?, station_id = ?, operator_id = ?, status = ?,
                      actual_start_time = COALESCE(actual_start_time, ?)
                    WHERE dispatch_id = ?""",
                    dispatchNo, taskId, workOrderId, plannedQty, stepId, stationId, assigneeId, status,
                    Timestamp.valueOf(LocalDateTime.now()), id);
        } else {
            jdbc.update("UPDATE dispatch_task SET dispatch_no = ?, task_id = ?, work_order_id = ?, planned_qty = ?, step_id = ?, station_id = ?, operator_id = ?, status = ? WHERE dispatch_id = ?",
                    dispatchNo, taskId, workOrderId, plannedQty, stepId, stationId, assigneeId, status, id);
        }
    }

    public void addCompletedQty(Long dispatchId, double qtyQualified) {
        if (dispatchId == null || qtyQualified <= 0) {
            return;
        }
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE dispatch_task SET
                  completed_qty = IFNULL(completed_qty, 0) + ?,
                  status = CASE
                    WHEN IFNULL(completed_qty, 0) + ? >= IFNULL(planned_qty, 0) THEN 'COMPLETED'
                    ELSE status
                  END,
                  actual_end_time = CASE
                    WHEN IFNULL(completed_qty, 0) + ? >= IFNULL(planned_qty, 0) THEN ?
                    ELSE actual_end_time
                  END
                WHERE dispatch_id = ?
                """,
                qtyQualified, qtyQualified, qtyQualified, Timestamp.valueOf(LocalDateTime.now()), dispatchId);
    }

    /** 按已执行报工汇总回写完成数，避免重复累加导致数量翻倍 */
    public void setCompletedQty(Long dispatchId, double completedQty) {
        if (dispatchId == null) {
            return;
        }
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE dispatch_task SET
                  completed_qty = ?,
                  status = CASE
                    WHEN ? >= IFNULL(planned_qty, 0) THEN 'COMPLETED'
                    ELSE status
                  END,
                  actual_end_time = CASE
                    WHEN ? >= IFNULL(planned_qty, 0) THEN COALESCE(actual_end_time, ?)
                    ELSE actual_end_time
                  END
                WHERE dispatch_id = ?
                """,
                completedQty, completedQty, completedQty,
                Timestamp.valueOf(LocalDateTime.now()), dispatchId);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM dispatch_task WHERE dispatch_id = ?", id);
    }

    private DispatchTaskVO mapRow(ResultSet rs, int n) throws SQLException {
        return new DispatchTaskVO(
                rs.getLong("dispatch_id"),
                rs.getString("dispatch_no"),
                rs.getString("task_no"),
                longOrNull(rs, "task_id"),
                longOrNull(rs, "work_order_id"),
                rs.getString("work_order_no"),
                longOrNull(rs, "step_id"),
                rs.getString("step_code"),
                longOrNull(rs, "station_id"),
                rs.getString("station_code"),
                rs.getString("step_name"),
                rs.getString("station_name"),
                longOrNull(rs, "product_id"),
                rs.getString("product_code"),
                rs.getString("product_name"),
                rs.getString("line_name"),
                rs.getString("line_code"),
                longOrNull(rs, "operator_id"),
                rs.getString("username"),
                toLong(rs.getBigDecimal("planned_qty")),
                toLong(rs.getBigDecimal("completed_qty")),
                rs.getString("status"),
                tsOrNull(rs, "planned_start_time"),
                tsOrNull(rs, "planned_end_time"),
                tsOrNull(rs, "actual_start_time"),
                rs.getString("customer_order_no"),
                tsOrNull(rs, "created_at"));
    }

    private static Long longOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static Long toLong(BigDecimal bd) {
        return bd == null ? 0L : bd.longValue();
    }

    private static String tsOrNull(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime().toString();
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
