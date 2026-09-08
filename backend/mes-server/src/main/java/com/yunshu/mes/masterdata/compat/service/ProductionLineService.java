package com.yunshu.mes.masterdata.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductionLineService {

    private final JdbcTemplate jdbc;

    public ProductionLineService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        StringBuilder sql = new StringBuilder("""
                SELECT pl.line_id, pl.line_code, pl.line_name, pl.workshop_id, pl.rated_capacity, pl.status,
                       w.workshop_code, w.workshop_name
                FROM production_line pl
                JOIN workshop w ON pl.workshop_id = w.workshop_id WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("lineCode"))) {
            sql.append(" AND pl.line_code LIKE ?");
            args.add("%" + params.get("lineCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("workshopId"))) {
            sql.append(" AND pl.workshop_id = ?");
            args.add(Long.parseLong(params.get("workshopId")));
        }
        sql.append(" ORDER BY pl.line_id LIMIT ? OFFSET ?");
        args.add(ps);
        args.add(PageUtil.offset(pn, ps));
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM production_line pl WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("lineCode"))) {
            sql.append(" AND pl.line_code LIKE ?");
            args.add("%" + params.get("lineCode").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Map<String, Object> getById(Long id) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT pl.line_id, pl.line_code, pl.line_name, pl.workshop_id, pl.rated_capacity, pl.status,
                       w.workshop_code, w.workshop_name
                FROM production_line pl JOIN workshop w ON pl.workshop_id = w.workshop_id
                WHERE pl.line_id = ?
                """, (rs, n) -> mapRow(rs), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        jdbc.update("""
                INSERT INTO production_line (workshop_id, line_code, line_name, rated_capacity, status)
                VALUES (?, ?, ?, ?, ?)
                """,
                body.get("workshopId"), body.get("lineCode"), body.get("lineName"),
                body.get("ratedCapacity"), toStatus(body.get("enableFlag")));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return jdbc.update("""
                UPDATE production_line SET workshop_id=?, line_code=?, line_name=?, rated_capacity=?, status=?, updated_at=?
                WHERE line_id=?
                """,
                body.get("workshopId"), body.get("lineCode"), body.get("lineName"), body.get("ratedCapacity"),
                toStatus(body.get("enableFlag")), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    @Transactional
    public int delete(Long id) {
        return jdbc.update("DELETE FROM production_line WHERE line_id = ?", id);
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("lineId", rs.getLong("line_id"));
        row.put("lineCode", rs.getString("line_code"));
        row.put("lineName", rs.getString("line_name"));
        row.put("workshopId", rs.getLong("workshop_id"));
        row.put("workshopCode", rs.getString("workshop_code"));
        row.put("workshopName", rs.getString("workshop_name"));
        row.put("ratedCapacity", rs.getObject("rated_capacity"));
        row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
        return row;
    }

    private String toStatus(Object enableFlag) {
        return enableFlag == null || "Y".equalsIgnoreCase(String.valueOf(enableFlag)) ? "ENABLED" : "DISABLED";
    }
}
