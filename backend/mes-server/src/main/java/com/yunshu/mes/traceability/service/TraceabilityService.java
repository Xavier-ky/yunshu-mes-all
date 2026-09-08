package com.yunshu.mes.traceability.service;

import com.yunshu.mes.traceability.vo.ProductTraceVO;
import com.yunshu.mes.traceability.vo.TraceQueryLogVO;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 产品追溯查询：按产品码查生产链路，以及追溯查询日志。
 */
@Service
public class TraceabilityService {

    private static final String LOGS_SQL = """
            SELECT trace_query_id, query_type, query_key, query_time
            FROM trace_query_log ORDER BY query_time DESC
            """;
    private static final String TRACE_SQL = """
            SELECT s.sn_id, s.sn_code, p.product_name, w.work_order_no, s.status
            FROM product_sn s
            LEFT JOIN product p ON s.product_id = p.product_id
            LEFT JOIN work_order w ON s.work_order_id = w.work_order_id
            WHERE s.sn_code = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public TraceabilityService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<TraceQueryLogVO> listQueryLogs() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return List.of();
        }
        return jdbc.query(LOGS_SQL, (rs, n) -> new TraceQueryLogVO(
                rs.getLong("trace_query_id"),
                rs.getString("query_type"),
                rs.getString("query_key"),
                rs.getString("query_time")));
    }

    public Optional<ProductTraceVO> traceProduct(String snCode) {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return Optional.empty();
        }
        Optional<ProductTraceVO> result = jdbc.query(TRACE_SQL, (rs, n) -> new ProductTraceVO(
                rs.getLong("sn_id"),
                rs.getString("sn_code"),
                rs.getString("product_name"),
                rs.getString("work_order_no"),
                rs.getString("status")), snCode)
                .stream()
                .findFirst();
        result.ifPresent(vo -> writeQueryLog(jdbc, snCode, vo));
        return result;
    }

    private void writeQueryLog(JdbcTemplate jdbc, String snCode, ProductTraceVO vo) {
        try {
            Long snId = jdbc.query("""
                    SELECT sn_id FROM product_sn WHERE sn_code = ? LIMIT 1
                    """, (rs, n) -> rs.getLong("sn_id"), snCode).stream().findFirst().orElse(null);
            jdbc.update("""
                    INSERT INTO trace_query_log (query_type, query_key, sn_id, query_user_id, result_summary)
                    VALUES ('PRODUCT_SN', ?, ?, NULL, ?)
                    """, snCode, snId, "{\"productName\":\"" + vo.productName() + "\"}");
        } catch (Exception ignored) {
            // 日志写入失败不影响查询结果
        }
    }
}
