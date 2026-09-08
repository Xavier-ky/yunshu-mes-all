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
public class DvMachineryRepository {

    private static final String BASE = """
            SELECT machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec,
                   machinery_type_id, machinery_type_code, machinery_type_name,
                   workshop_id, workshop_code, workshop_name,
                   last_mainten_time, last_check_time, status,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_machinery
            """;

    private final JdbcTemplate jdbc;

    public DvMachineryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY machinery_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapMachinery(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_machinery WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE machinery_id = ?",
                (rs, n) -> DvSchemas.mapMachinery(rs), id);
        return rows.stream().findFirst();
    }

    public long countByTypeId(Long typeId) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_machinery WHERE machinery_type_id = ?",
                Long.class, typeId);
        return c == null ? 0 : c;
    }

    public boolean codeExists(String code, Long excludeId) {
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_machinery WHERE machinery_code = ?",
                    Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dv_machinery WHERE machinery_code = ? AND machinery_id <> ?",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_machinery (
                      machinery_code, machinery_name, machinery_brand, machinery_spec,
                      machinery_type_id, machinery_type_code, machinery_type_name,
                      workshop_id, workshop_code, workshop_name,
                      last_mainten_time, last_check_time, status, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryBrand")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machinerySpec")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("machineryTypeId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryTypeCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryTypeName")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("workshopId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("workshopCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("workshopName")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("lastMaintenTime")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("lastCheckTime")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("status"), "STOP"));
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
                UPDATE dv_machinery SET
                  machinery_code=?, machinery_name=?, machinery_brand=?, machinery_spec=?,
                  machinery_type_id=?, machinery_type_code=?, machinery_type_name=?,
                  workshop_id=?, workshop_code=?, workshop_name=?,
                  last_mainten_time=?, last_check_time=?, status=?, remark=?,
                  update_by=?, update_time=NOW(3)
                WHERE machinery_id=?
                """,
                DvJdbcHelper.str(body.get("machineryCode")),
                DvJdbcHelper.str(body.get("machineryName")),
                DvJdbcHelper.str(body.get("machineryBrand")),
                DvJdbcHelper.str(body.get("machinerySpec")),
                DvJdbcHelper.longObj(body.get("machineryTypeId")),
                DvJdbcHelper.str(body.get("machineryTypeCode")),
                DvJdbcHelper.str(body.get("machineryTypeName")),
                DvJdbcHelper.longObj(body.get("workshopId")),
                DvJdbcHelper.str(body.get("workshopCode")),
                DvJdbcHelper.str(body.get("workshopName")),
                DvJdbcHelper.parseTs(body.get("lastMaintenTime")),
                DvJdbcHelper.parseTs(body.get("lastCheckTime")),
                DvJdbcHelper.str(body.get("status")),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("machineryId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_machinery WHERE machinery_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.like(sql, args, "machinery_code", params.get("machineryCode"));
        DvJdbcHelper.like(sql, args, "machinery_name", params.get("machineryName"));
        DvJdbcHelper.eq(sql, args, "machinery_type_code", params.get("machineryTypeCode"));
        DvJdbcHelper.eq(sql, args, "workshop_code", params.get("workshopCode"));
        DvJdbcHelper.eq(sql, args, "status", params.get("status"));
        DvJdbcHelper.eqLong(sql, args, "machinery_type_id", params, "machineryTypeId");
        DvJdbcHelper.eqLong(sql, args, "workshop_id", params, "workshopId");
    }
}
