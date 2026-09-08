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

@Repository
public class DvCheckRecordRepository {

    private static final String BASE = """
            SELECT record_id, plan_id, plan_code, plan_name, plan_type,
                   machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
                   check_time, user_id, user_name, nick_name, status,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_check_record
            """;

    private final JdbcTemplate jdbc;

    public DvCheckRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY record_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapCheckRecord(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_check_record WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE record_id = ?",
                (rs, n) -> DvSchemas.mapCheckRecord(rs), id);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_check_record (
                      plan_id, plan_code, plan_name, plan_type,
                      machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
                      check_time, user_id, user_name, nick_name, status, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("planId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("planCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("planName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("planType")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("machineryId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryBrand")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machinerySpec")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("checkTime")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("userId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("userName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("nickName")));
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
                UPDATE dv_check_record SET
                  plan_id=?, plan_code=?, plan_name=?, plan_type=?,
                  machinery_id=?, machinery_code=?, machinery_name=?, machinery_brand=?, machinery_spec=?,
                  check_time=?, user_id=?, user_name=?, nick_name=?, status=?, remark=?,
                  update_by=?, update_time=NOW(3)
                WHERE record_id=?
                """,
                DvJdbcHelper.longObj(body.get("planId")),
                DvJdbcHelper.str(body.get("planCode")),
                DvJdbcHelper.str(body.get("planName")),
                DvJdbcHelper.str(body.get("planType")),
                DvJdbcHelper.longObj(body.get("machineryId")),
                DvJdbcHelper.str(body.get("machineryCode")),
                DvJdbcHelper.str(body.get("machineryName")),
                DvJdbcHelper.str(body.get("machineryBrand")),
                DvJdbcHelper.str(body.get("machinerySpec")),
                DvJdbcHelper.parseTs(body.get("checkTime")),
                DvJdbcHelper.longObj(body.get("userId")),
                DvJdbcHelper.str(body.get("userName")),
                DvJdbcHelper.str(body.get("nickName")),
                DvJdbcHelper.str(body.get("status")),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("recordId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_check_record WHERE record_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.eqLong(sql, args, "plan_id", params, "planId");
        DvJdbcHelper.eqLong(sql, args, "machinery_id", params, "machineryId");
        DvJdbcHelper.like(sql, args, "machinery_code", params.get("machineryCode"));
        DvJdbcHelper.like(sql, args, "machinery_name", params.get("machineryName"));
        DvJdbcHelper.eq(sql, args, "status", params.get("status"));
    }
}
