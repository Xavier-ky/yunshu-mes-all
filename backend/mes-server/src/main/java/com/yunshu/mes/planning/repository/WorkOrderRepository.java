package com.yunshu.mes.planning.repository;

import com.yunshu.mes.planning.vo.WorkOrderVO;
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
 * 生产工单数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class WorkOrderRepository {

    private static final String SELECT_LIST = """
            SELECT wo.work_order_id, wo.work_order_no, wo.order_id, co.order_no,
                   wo.product_id, p.product_name, wo.plan_qty, wo.completed_qty, wo.status,
                   ka.analysis_status AS kitting_status
            FROM work_order wo
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN customer_order co ON wo.order_id = co.order_id
            LEFT JOIN (
                SELECT work_order_id, analysis_status
                FROM kitting_analysis ka1
                WHERE analysis_id = (
                    SELECT MAX(analysis_id) FROM kitting_analysis ka2 WHERE ka2.work_order_id = ka1.work_order_id
                )
            ) ka ON ka.work_order_id = wo.work_order_id
            ORDER BY wo.work_order_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT wo.work_order_id, wo.work_order_no, wo.order_id, co.order_no,
                   wo.product_id, p.product_name, wo.plan_qty, wo.completed_qty, wo.status,
                   ka.analysis_status AS kitting_status
            FROM work_order wo
            LEFT JOIN product p ON wo.product_id = p.product_id
            LEFT JOIN customer_order co ON wo.order_id = co.order_id
            LEFT JOIN (
                SELECT work_order_id, analysis_status
                FROM kitting_analysis ka1
                WHERE analysis_id = (
                    SELECT MAX(analysis_id) FROM kitting_analysis ka2 WHERE ka2.work_order_id = ka1.work_order_id
                )
            ) ka ON ka.work_order_id = wo.work_order_id
            WHERE wo.work_order_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public WorkOrderRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<WorkOrderVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, this::mapRow);
    }

    public Optional<WorkOrderVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<WorkOrderVO> list = jdbc.query(SELECT_BY_ID, this::mapRow, id);
        return list.stream().findFirst();
    }

    private WorkOrderVO mapRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new WorkOrderVO(
                rs.getLong("work_order_id"),
                rs.getString("work_order_no"),
                rs.getObject("order_id") != null ? rs.getLong("order_id") : null,
                rs.getString("order_no"),
                rs.getLong("product_id"),
                rs.getString("product_name"),
                rs.getLong("plan_qty"),
                rs.getLong("completed_qty"),
                rs.getString("status"),
                rs.getString("kitting_status"));
    }

    public Long insert(String workOrderNo, Long productId, Long orderId, Long orderItemId, Long planQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO work_order (work_order_no, product_id, order_id, order_item_id, plan_qty, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, workOrderNo);
            ps.setLong(2, productId);
            if (orderId != null) {
                ps.setLong(3, orderId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (orderItemId != null) {
                ps.setLong(4, orderItemId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setLong(5, planQty);
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String workOrderNo, Long productId, Long orderId, Long planQty, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE work_order SET work_order_no = ?, product_id = ?, order_id = ?, plan_qty = ?, status = ? WHERE work_order_id = ?",
                workOrderNo, productId, orderId, planQty, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM work_order WHERE work_order_id = ?", id);
    }

    public void enrichFromOrder(Long workOrderId, Long orderId) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE work_order wo
                JOIN customer_order co ON co.order_id = ?
                SET wo.source_code = co.order_no,
                    wo.client_name = co.customer_name,
                    wo.work_order_name = CONCAT('工单-', co.order_no),
                    wo.order_source = 'ORDER'
                WHERE wo.work_order_id = ?
                """, orderId, workOrderId);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
