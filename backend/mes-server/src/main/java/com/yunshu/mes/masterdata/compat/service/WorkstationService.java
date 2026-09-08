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
public class WorkstationService {

    private final JdbcTemplate jdbc;

    public WorkstationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendLike(sql, args, params.get("workstationCode"), "ws.station_code");
        appendLike(sql, args, params.get("workstationName"), "ws.station_name");
        if (StringUtils.hasText(params.get("workshopId"))) {
            sql.append(" AND w.workshop_id = ?");
            args.add(Long.parseLong(params.get("workshopId")));
        }
        sql.append(" ORDER BY ws.station_id LIMIT ? OFFSET ?");
        args.add(ps);
        args.add(PageUtil.offset(pn, ps));
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM workstation ws
                JOIN production_line pl ON ws.line_id = pl.line_id
                JOIN workshop w ON pl.workshop_id = w.workshop_id WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendLike(sql, args, params.get("workstationCode"), "ws.station_code");
        appendLike(sql, args, params.get("workstationName"), "ws.station_name");
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Map<String, Object> getById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE_SELECT + " WHERE ws.station_id = ?",
                (rs, n) -> mapRow(rs), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        Long lineId = body.get("lineId") != null ? Long.parseLong(String.valueOf(body.get("lineId")))
                : firstLineId(body.get("workshopId"));
        jdbc.update("""
                INSERT INTO workstation (line_id, station_code, station_name, station_type, status)
                VALUES (?, ?, ?, ?, ?)
                """,
                lineId, body.get("workstationCode"), body.get("workstationName"),
                body.getOrDefault("processCode", "ASSEMBLY"), toStatus(body.get("enableFlag")));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("workstationId")));
        return jdbc.update("""
                UPDATE workstation SET station_code=?, station_name=?, station_type=?, status=?, updated_at=?
                WHERE station_id=?
                """,
                body.get("workstationCode"), body.get("workstationName"),
                body.getOrDefault("processCode", "ASSEMBLY"), toStatus(body.get("enableFlag")),
                Timestamp.valueOf(LocalDateTime.now()), id);
    }

    @Transactional
    public int delete(Long id) {
        return jdbc.update("DELETE FROM workstation WHERE station_id = ?", id);
    }

    private Long firstLineId(Object workshopId) {
        if (workshopId == null) {
            return jdbc.queryForObject("SELECT line_id FROM production_line ORDER BY line_id LIMIT 1", Long.class);
        }
        List<Long> ids = jdbc.queryForList(
                "SELECT line_id FROM production_line WHERE workshop_id = ? ORDER BY line_id LIMIT 1",
                Long.class, Long.parseLong(String.valueOf(workshopId)));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("车间下无产线，请先创建产线");
        }
        return ids.get(0);
    }

    private static final String BASE_SELECT = """
            SELECT ws.station_id AS workstation_id, ws.station_code AS workstation_code,
                   ws.station_name AS workstation_name, ws.station_type AS process_code, ws.status,
                   pl.line_id, pl.line_name, w.workshop_id, w.workshop_code, w.workshop_name
            FROM workstation ws
            JOIN production_line pl ON ws.line_id = pl.line_id
            JOIN workshop w ON pl.workshop_id = w.workshop_id
            """;

    private Map<String, Object> mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workstationId", rs.getLong("workstation_id"));
        row.put("workstationCode", rs.getString("workstation_code"));
        row.put("workstationName", rs.getString("workstation_name"));
        row.put("processCode", rs.getString("process_code"));
        row.put("lineId", rs.getLong("line_id"));
        row.put("lineName", rs.getString("line_name"));
        row.put("workshopId", rs.getLong("workshop_id"));
        row.put("workshopCode", rs.getString("workshop_code"));
        row.put("workshopName", rs.getString("workshop_name"));
        row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
        row.put("warehouseId", 1);
        row.put("locationId", 1);
        row.put("areaId", 1);
        return row;
    }

    private String toStatus(Object enableFlag) {
        return enableFlag == null || "Y".equalsIgnoreCase(String.valueOf(enableFlag)) ? "ENABLED" : "DISABLED";
    }

    private void appendLike(StringBuilder sql, List<Object> args, String val, String col) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" LIKE ?");
            args.add("%" + val.trim() + "%");
        }
    }
}
