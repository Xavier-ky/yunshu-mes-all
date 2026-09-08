package com.yunshu.mes.planning.repository;

import com.yunshu.mes.planning.vo.ProductionTaskVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 生产任务数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class ProductionTaskRepository {

    private static final String SELECT_LIST = """
            SELECT pt.task_id, pt.task_no, pt.work_order_id, wo.work_order_no, l.line_name, pt.task_qty, pt.status
            FROM production_task pt
            LEFT JOIN work_order wo ON pt.work_order_id = wo.work_order_id
            LEFT JOIN production_line l ON pt.line_id = l.line_id
            ORDER BY pt.task_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT pt.task_id, pt.task_no, pt.work_order_id, wo.work_order_no, l.line_name, pt.task_qty, pt.status
            FROM production_task pt
            LEFT JOIN work_order wo ON pt.work_order_id = wo.work_order_id
            LEFT JOIN production_line l ON pt.line_id = l.line_id
            WHERE pt.task_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductionTaskRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProductionTaskVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProductionTaskVO(
                rs.getLong("task_id"),
                rs.getString("task_no"),
                rs.getLong("work_order_id"),
                rs.getString("work_order_no"),
                rs.getString("line_name"),
                rs.getLong("task_qty"),
                rs.getString("status")));
    }

    public Optional<ProductionTaskVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProductionTaskVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProductionTaskVO(
                rs.getLong("task_id"),
                rs.getString("task_no"),
                rs.getLong("work_order_id"),
                rs.getString("work_order_no"),
                rs.getString("line_name"),
                rs.getLong("task_qty"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String taskNo, Long workOrderId, Long lineId, Long planQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO production_task (task_no, work_order_id, line_id, task_date, task_qty, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, taskNo);
            ps.setLong(2, workOrderId);
            if (lineId != null) {
                ps.setLong(3, lineId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            ps.setDate(4, new java.sql.Date(System.currentTimeMillis()));
            ps.setLong(5, planQty);
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String taskNo, Long workOrderId, Long lineId, Long planQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE production_task SET task_no = ?, work_order_id = ?, line_id = ?, task_qty = ?, status = ? WHERE task_id = ?",
                taskNo, workOrderId, lineId, planQty, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM production_task WHERE task_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
