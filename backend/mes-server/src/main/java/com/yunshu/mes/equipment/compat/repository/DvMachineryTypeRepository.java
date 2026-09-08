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
public class DvMachineryTypeRepository {

    private static final String BASE = """
            SELECT machinery_type_id, machinery_type_code, machinery_type_name, parent_type_id, ancestors,
                   enable_flag, remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_machinery_type
            """;

    private final JdbcTemplate jdbc;

    public DvMachineryTypeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAll(Map<String, String> params) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        DvJdbcHelper.like(sql, args, "machinery_type_code", params.get("machineryTypeCode"));
        DvJdbcHelper.like(sql, args, "machinery_type_name", params.get("machineryTypeName"));
        DvJdbcHelper.eq(sql, args, "enable_flag", params.get("enableFlag"));
        sql.append(" ORDER BY machinery_type_id");
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapMachineryType(rs), args.toArray());
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE machinery_type_id = ?",
                (rs, n) -> DvSchemas.mapMachineryType(rs), id);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_machinery_type (
                      machinery_type_code, machinery_type_name, parent_type_id, ancestors,
                      enable_flag, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryTypeCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryTypeName")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("parentTypeId")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("ancestors"), "0"));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("enableFlag"), "Y"));
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
                UPDATE dv_machinery_type SET
                  machinery_type_code=?, machinery_type_name=?, parent_type_id=?, ancestors=?,
                  enable_flag=?, remark=?, update_by=?, update_time=NOW(3)
                WHERE machinery_type_id=?
                """,
                DvJdbcHelper.str(body.get("machineryTypeCode")),
                DvJdbcHelper.str(body.get("machineryTypeName")),
                DvJdbcHelper.longObj(body.get("parentTypeId")),
                DvJdbcHelper.strOr(body.get("ancestors"), "0"),
                DvJdbcHelper.strOr(body.get("enableFlag"), "Y"),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("machineryTypeId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_machinery_type WHERE machinery_type_id IN (" + ph + ")", ids.toArray());
    }
}
