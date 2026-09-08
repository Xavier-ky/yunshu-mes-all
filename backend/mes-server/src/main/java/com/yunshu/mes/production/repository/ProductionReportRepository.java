package com.yunshu.mes.production.repository;

import com.yunshu.mes.production.vo.ProductionReportVO;
import java.sql.PreparedStatement;
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
 * 生产报工数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class ProductionReportRepository {

    private static final String SELECT_LIST = """
            SELECT r.report_id, r.report_no, w.work_order_no, s.step_name, r.report_type,
                   r.good_qty, r.defect_qty, r.report_time
            FROM production_report r
            LEFT JOIN work_order w ON r.work_order_id = w.work_order_id
            LEFT JOIN process_step s ON r.step_id = s.step_id
            ORDER BY r.report_id DESC
            """;
    private static final String SELECT_BY_ID = """
            SELECT r.report_id, r.report_no, w.work_order_no, s.step_name, r.report_type,
                   r.good_qty, r.defect_qty, r.report_time
            FROM production_report r
            LEFT JOIN work_order w ON r.work_order_id = w.work_order_id
            LEFT JOIN process_step s ON r.step_id = s.step_id
            WHERE r.report_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductionReportRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ProductionReportVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new ProductionReportVO(
                rs.getLong("report_id"),
                rs.getString("report_no"),
                rs.getString("work_order_no"),
                rs.getString("step_name"),
                rs.getString("report_type"),
                rs.getString("good_qty"),
                rs.getString("defect_qty"),
                rs.getString("report_time")));
    }

    public Optional<ProductionReportVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<ProductionReportVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new ProductionReportVO(
                rs.getLong("report_id"),
                rs.getString("report_no"),
                rs.getString("work_order_no"),
                rs.getString("step_name"),
                rs.getString("report_type"),
                rs.getString("good_qty"),
                rs.getString("defect_qty"),
                rs.getString("report_time")), id);
        return list.stream().findFirst();
    }

    public Long insert(String reportNo, Long workOrderId, Long stepId, Long operatorId,
                       String reportType, String goodQty, String defectQty, String remark) {
        return insertFull(reportNo, null, workOrderId, stepId, null, operatorId, null,
                reportType, goodQty, defectQty, remark);
    }

    public Long insertFull(String reportNo, Long dispatchId, Long workOrderId, Long stepId,
                           Long stationId, Long operatorId, Long snId,
                           String reportType, String goodQty, String defectQty, String remark) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO production_report
                    (report_no, dispatch_id, work_order_id, step_id, station_id, operator_id, sn_id,
                     report_type, good_qty, defect_qty, report_time, remark)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reportNo);
            ps.setObject(2, dispatchId);
            ps.setObject(3, workOrderId);
            ps.setObject(4, stepId);
            ps.setObject(5, stationId);
            ps.setObject(6, operatorId);
            ps.setObject(7, snId);
            ps.setString(8, reportType);
            ps.setString(9, goodQty);
            ps.setString(10, defectQty);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(12, remark);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String reportNo, Long workOrderId, Long stepId,
                       String reportType, String goodQty, String defectQty, String remark) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE production_report SET report_no = ?, work_order_id = ?, step_id = ?, report_type = ?, good_qty = ?, defect_qty = ?, remark = ? WHERE report_id = ?",
                reportNo, workOrderId, stepId, reportType, goodQty, defectQty, remark, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM production_report WHERE report_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
