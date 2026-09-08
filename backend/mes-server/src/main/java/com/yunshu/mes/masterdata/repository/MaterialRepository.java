package com.yunshu.mes.masterdata.repository;

import com.yunshu.mes.masterdata.vo.MaterialVO;
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
 * 物料数据访问 — JdbcTemplate，不可用时抛异常由上层回退 Mock。
 */
@Repository
public class MaterialRepository {

    private static final String SELECT_LIST = """
            SELECT m.material_id, m.material_code, m.material_name, m.material_type,
                   u.unit_name, m.is_key_material, m.status
            FROM material m
            LEFT JOIN uom u ON m.unit_id = u.unit_id
            ORDER BY m.material_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT m.material_id, m.material_code, m.material_name, m.material_type,
                   u.unit_name, m.is_key_material, m.status
            FROM material m
            LEFT JOIN uom u ON m.unit_id = u.unit_id
            WHERE m.material_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public MaterialRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<MaterialVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new MaterialVO(
                rs.getLong("material_id"),
                rs.getString("material_code"),
                rs.getString("material_name"),
                rs.getString("material_type"),
                rs.getString("unit_name"),
                rs.getString("is_key_material"),
                rs.getString("status")));
    }

    public Optional<MaterialVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<MaterialVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new MaterialVO(
                rs.getLong("material_id"),
                rs.getString("material_code"),
                rs.getString("material_name"),
                rs.getString("material_type"),
                rs.getString("unit_name"),
                rs.getString("is_key_material"),
                rs.getString("status")), id);
        return list.stream().findFirst();
    }

    public Long insert(String materialCode, String materialName, String materialType, Long unitId, Integer isCritical, String status) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO material (material_code, material_name, material_type, unit_id, is_key_material, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, materialCode);
            ps.setString(2, materialName);
            ps.setString(3, materialType);
            if (unitId != null) {
                ps.setLong(4, unitId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            if (isCritical != null) {
                ps.setInt(5, isCritical);
            } else {
                ps.setNull(5, java.sql.Types.TINYINT);
            }
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String materialCode, String materialName, String materialType, Long unitId, Integer isCritical, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE material SET material_code = ?, material_name = ?, material_type = ?, unit_id = ?, is_key_material = ?, status = ? WHERE material_id = ?",
                materialCode, materialName, materialType, unitId, isCritical, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM material WHERE material_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
