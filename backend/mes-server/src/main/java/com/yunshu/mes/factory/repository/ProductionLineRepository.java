package com.yunshu.mes.factory.repository;

import com.yunshu.mes.factory.vo.LineVO;
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
 * 产线数据访问。
 */
@Repository
public class ProductionLineRepository {

    private static final String SELECT_LIST = """
            SELECT l.line_id, l.line_code, l.line_name, w.workshop_name,
                   l.rated_capacity, l.capacity_unit, l.status,
                   l.model_pos_x, l.model_pos_y, l.model_pos_z
            FROM production_line l
            LEFT JOIN workshop w ON l.workshop_id = w.workshop_id
            ORDER BY l.line_id
            """;
    private static final String SELECT_BY_ID = """
            SELECT l.line_id, l.line_code, l.line_name, w.workshop_name,
                   l.rated_capacity, l.capacity_unit, l.status,
                   l.model_pos_x, l.model_pos_y, l.model_pos_z
            FROM production_line l
            LEFT JOIN workshop w ON l.workshop_id = w.workshop_id
            WHERE l.line_id = ?
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ProductionLineRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<LineVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new LineVO(
                rs.getLong("line_id"),
                rs.getString("line_code"),
                rs.getString("line_name"),
                rs.getString("workshop_name"),
                rs.getString("rated_capacity"),
                rs.getString("capacity_unit"),
                rs.getString("status"),
                rs.getObject("model_pos_x") != null ? rs.getDouble("model_pos_x") : null,
                rs.getObject("model_pos_y") != null ? rs.getDouble("model_pos_y") : null,
                rs.getObject("model_pos_z") != null ? rs.getDouble("model_pos_z") : null));
    }

    public Optional<LineVO> findById(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        List<LineVO> list = jdbc.query(SELECT_BY_ID, (rs, n) -> new LineVO(
                rs.getLong("line_id"),
                rs.getString("line_code"),
                rs.getString("line_name"),
                rs.getString("workshop_name"),
                rs.getString("rated_capacity"),
                rs.getString("capacity_unit"),
                rs.getString("status"),
                rs.getObject("model_pos_x") != null ? rs.getDouble("model_pos_x") : null,
                rs.getObject("model_pos_y") != null ? rs.getDouble("model_pos_y") : null,
                rs.getObject("model_pos_z") != null ? rs.getDouble("model_pos_z") : null), id);
        return list.stream().findFirst();
    }

    public Long insert(Long workshopId, String lineCode, String lineName,
                       String ratedCapacity, String capacityUnit, String status,
                       Double modelPosX, Double modelPosY, Double modelPosZ) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO production_line (workshop_id, line_code, line_name, rated_capacity, capacity_unit, status, "
                            + "model_pos_x, model_pos_y, model_pos_z) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, workshopId);
            ps.setString(2, lineCode);
            ps.setString(3, lineName);
            ps.setString(4, ratedCapacity);
            ps.setString(5, capacityUnit);
            ps.setString(6, status);
            if (modelPosX != null) ps.setDouble(7, modelPosX); else ps.setNull(7, java.sql.Types.DECIMAL);
            if (modelPosY != null) ps.setDouble(8, modelPosY); else ps.setNull(8, java.sql.Types.DECIMAL);
            if (modelPosZ != null) ps.setDouble(9, modelPosZ); else ps.setNull(9, java.sql.Types.DECIMAL);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, Long workshopId, String lineCode, String lineName,
                       String ratedCapacity, String capacityUnit, String status) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                UPDATE production_line
                SET workshop_id = ?, line_code = ?, line_name = ?, rated_capacity = ?, capacity_unit = ?, status = ?
                WHERE line_id = ?
                """, workshopId, lineCode, lineName, ratedCapacity, capacityUnit, status, id);
    }

    public void delete(Long id) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("DELETE FROM production_line WHERE line_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置，回退 Mock");
        }
        return jdbc;
    }
}
