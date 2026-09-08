package com.yunshu.mes.masterdata.repository;

import com.yunshu.mes.masterdata.vo.BomItemVO;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class BomItemRepository {

    private static final String SELECT_LIST = """
            SELECT bi.bom_item_id, bi.bom_id, bi.material_id,
                   m.material_name, m.material_code,
                   bi.qty_per, bi.loss_rate, bi.is_key_material, bi.remark
            FROM bom_item bi LEFT JOIN material m ON bi.material_id = m.material_id
            ORDER BY bi.bom_item_id
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public BomItemRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<BomItemVO> findByBomId(Long bomId) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(
            "SELECT bi.bom_item_id, bi.bom_id, bi.material_id, m.material_name, m.material_code, bi.qty_per, bi.loss_rate, bi.is_key_material, bi.remark FROM bom_item bi LEFT JOIN material m ON bi.material_id = m.material_id WHERE bi.bom_id = ? ORDER BY bi.bom_item_id",
            (rs, n) -> new BomItemVO(rs.getLong("bom_item_id"), rs.getLong("bom_id"), rs.getLong("material_id"),
                rs.getString("material_name"), rs.getString("material_code"),
                rs.getBigDecimal("qty_per"), rs.getBigDecimal("loss_rate"),
                rs.getBoolean("is_key_material"), rs.getString("remark")),
            bomId);
    }

    public Long insert(Long bomId, Long materialId, BigDecimal qtyPer, BigDecimal lossRate, Boolean isKeyMaterial, String remark) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bom_item (bom_id, material_id, qty_per, loss_rate, is_key_material, remark) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, bomId); ps.setLong(2, materialId);
            ps.setBigDecimal(3, qtyPer); ps.setBigDecimal(4, lossRate);
            ps.setBoolean(5, isKeyMaterial); ps.setString(6, remark);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Long id, Long materialId, BigDecimal qtyPer, BigDecimal lossRate, Boolean isKeyMaterial, String remark) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE bom_item SET material_id=?, qty_per=?, loss_rate=?, is_key_material=?, remark=? WHERE bom_item_id=?",
            materialId, qtyPer, lossRate, isKeyMaterial, remark, id);
    }

    public void delete(Long id) {
        requireJdbc().update("DELETE FROM bom_item WHERE bom_item_id = ?", id);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        return jdbc;
    }
}
