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
public class WorkshopService {

    private final JdbcTemplate jdbc;

    public WorkshopService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        StringBuilder sql = new StringBuilder("""
                SELECT workshop_id, workshop_code, workshop_name, area, charge, status, remark
                FROM workshop WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendLike(sql, args, params.get("workshopCode"), "workshop_code");
        appendLike(sql, args, params.get("workshopName"), "workshop_name");
        sql.append(" ORDER BY workshop_id LIMIT ? OFFSET ?");
        args.add(ps);
        args.add(PageUtil.offset(pn, ps));
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM workshop WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendLike(sql, args, params.get("workshopCode"), "workshop_code");
        appendLike(sql, args, params.get("workshopName"), "workshop_name");
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.query("""
                SELECT workshop_id, workshop_code, workshop_name, area, charge, status, remark
                FROM workshop ORDER BY workshop_id
                """, (rs, n) -> mapRow(rs));
    }

    public Map<String, Object> getById(Long id) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT workshop_id, workshop_code, workshop_name, area, charge, status, remark
                FROM workshop WHERE workshop_id = ?
                """, (rs, n) -> mapRow(rs), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        String status = toStatus(body.get("enableFlag"));
        jdbc.update("""
                INSERT INTO workshop (workshop_code, workshop_name, area, charge, status, remark)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                body.get("workshopCode"), body.get("workshopName"), body.get("area"), body.get("charge"),
                status, body.getOrDefault("remark", ""));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id;
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("workshopId")));
        return jdbc.update("""
                UPDATE workshop SET workshop_code=?, workshop_name=?, area=?, charge=?, status=?, remark=?, updated_at=?
                WHERE workshop_id=?
                """,
                body.get("workshopCode"), body.get("workshopName"), body.get("area"), body.get("charge"),
                toStatus(body.get("enableFlag")), body.getOrDefault("remark", ""),
                Timestamp.valueOf(LocalDateTime.now()), id);
    }

    @Transactional
    public int delete(Long id) {
        return jdbc.update("DELETE FROM workshop WHERE workshop_id = ?", id);
    }

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workshopId", rs.getLong("workshop_id"));
        row.put("workshopCode", rs.getString("workshop_code"));
        row.put("workshopName", rs.getString("workshop_name"));
        row.put("area", rs.getObject("area"));
        row.put("charge", rs.getString("charge"));
        row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
        row.put("remark", rs.getString("remark"));
        return row;
    }

    private String toStatus(Object enableFlag) {
        if (enableFlag == null) {
            return "ENABLED";
        }
        return "Y".equalsIgnoreCase(String.valueOf(enableFlag)) ? "ENABLED" : "DISABLED";
    }

    private void appendLike(StringBuilder sql, List<Object> args, String val, String col) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" LIKE ?");
            args.add("%" + val.trim() + "%");
        }
    }
}
