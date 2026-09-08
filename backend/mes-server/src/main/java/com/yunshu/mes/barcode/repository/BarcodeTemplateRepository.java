package com.yunshu.mes.barcode.repository;

import com.yunshu.mes.barcode.vo.BarcodeTemplateVO;
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
 * 条码模板数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class BarcodeTemplateRepository {

    private static final String SELECT_LIST = """
            SELECT template_id, template_code, template_name, status
            FROM barcode_template ORDER BY template_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT template_id, template_code, template_name, status
            FROM barcode_template WHERE template_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public BarcodeTemplateRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<BarcodeTemplateVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new BarcodeTemplateVO(
                rs.getLong("template_id"),
                rs.getString("template_code"),
                rs.getString("template_name"),
                rs.getString("status")));
    }

    public Optional<BarcodeTemplateVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<BarcodeTemplateVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new BarcodeTemplateVO(
                rs.getLong("template_id"),
                rs.getString("template_code"),
                rs.getString("template_name"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String templateCode, String templateName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO barcode_template (template_code, template_name, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, templateCode);
            ps.setString(2, templateName);
            ps.setString(3, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String templateCode, String templateName, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE barcode_template SET template_code = ?, template_name = ?, status = ? WHERE template_id = ?",
                templateCode, templateName, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM barcode_template WHERE template_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
