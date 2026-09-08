package com.yunshu.mes.equipment.compat.repository;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.DvSchemas;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class DvCheckPlanRepository {

    private static final String BASE = """
            SELECT plan_id, plan_code, plan_name, plan_type, start_date, end_date,
                   cycle_type, cycle_count, status,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_check_plan
            """;

    private final JdbcTemplate jdbc;

    public DvCheckPlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY plan_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapCheckPlan(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_check_plan WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE plan_id = ?",
                (rs, n) -> DvSchemas.mapCheckPlan(rs), id);
        return rows.stream().findFirst();
    }

    public boolean codeExists(String code, Long excludeId) {
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_check_plan WHERE plan_code = ?",
                    Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dv_check_plan WHERE plan_code = ? AND plan_id <> ?",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public List<Map<String, Object>> findByPlanIdsAndType(List<Long> planIds, String planType) {
        if (planIds == null || planIds.isEmpty()) {
            return List.of();
        }
        String ph = String.join(",", planIds.stream().map(x -> "?").toList());
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE plan_id IN (").append(ph).append(")");
        List<Object> args = new ArrayList<>(planIds);
        if (StringUtils.hasText(planType)) {
            sql.append(" AND plan_type = ?");
            args.add(planType.trim());
        }
        sql.append(" ORDER BY plan_id");
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapCheckPlan(rs), args.toArray());
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_check_plan (
                      plan_code, plan_name, plan_type, start_date, end_date,
                      cycle_type, cycle_count, status, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, DvJdbcHelper.str(body.get("planCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("planName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("planType")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("startDate")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("endDate")));
            ps.setString(i++, DvJdbcHelper.str(body.get("cycleType")));
            ps.setObject(i++, DvJdbcHelper.intObj(body.get("cycleCount")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("status"), "PREPARE"));
            ps.setString(i++, DvJdbcHelper.str(body.get("remark")));
            ps.setString(i++, DvJdbcHelper.str(body.get("createBy")));
            ps.setTimestamp(i, DvJdbcHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        return jdbc.update("""
                UPDATE dv_check_plan SET
                  plan_code=?, plan_name=?, plan_type=?, start_date=?, end_date=?,
                  cycle_type=?, cycle_count=?, status=?, remark=?,
                  update_by=?, update_time=NOW(3)
                WHERE plan_id=?
                """,
                DvJdbcHelper.str(body.get("planCode")),
                DvJdbcHelper.str(body.get("planName")),
                DvJdbcHelper.str(body.get("planType")),
                DvJdbcHelper.parseTs(body.get("startDate")),
                DvJdbcHelper.parseTs(body.get("endDate")),
                DvJdbcHelper.str(body.get("cycleType")),
                DvJdbcHelper.intObj(body.get("cycleCount")),
                DvJdbcHelper.str(body.get("status")),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("planId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_check_plan WHERE plan_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.like(sql, args, "plan_code", params.get("planCode"));
        DvJdbcHelper.like(sql, args, "plan_name", params.get("planName"));
        DvJdbcHelper.eq(sql, args, "plan_type", params.get("planType"));
        DvJdbcHelper.eq(sql, args, "status", params.get("status"));
    }
}
