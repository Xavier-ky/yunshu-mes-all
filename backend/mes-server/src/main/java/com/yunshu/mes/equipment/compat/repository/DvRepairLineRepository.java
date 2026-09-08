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
public class DvRepairLineRepository {

    private static final String BASE = """
            SELECT line_id, repair_id, subject_id, subject_code, subject_name, subject_type,
                   subject_content, subject_standard, malfunction, malfunction_url, repair_des,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_repair_line
            """;

    private final JdbcTemplate jdbc;

    public DvRepairLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY line_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapRepairLine(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_repair_line WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE line_id = ?",
                (rs, n) -> DvSchemas.mapRepairLine(rs), id);
        return rows.stream().findFirst();
    }

    public int deleteByRepairId(Long repairId) {
        return jdbc.update("DELETE FROM dv_repair_line WHERE repair_id = ?", repairId);
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_repair_line (
                      repair_id, subject_id, subject_code, subject_name, subject_type,
                      subject_content, subject_standard, malfunction, malfunction_url, repair_des,
                      remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("repairId")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("subjectId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectType")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectContent")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectStandard")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("malfunction"), ""));
            ps.setString(i++, DvJdbcHelper.str(body.get("malfunctionUrl")));
            ps.setString(i++, DvJdbcHelper.str(body.get("repairDes")));
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
                UPDATE dv_repair_line SET
                  repair_id=?, subject_id=?, subject_code=?, subject_name=?, subject_type=?,
                  subject_content=?, subject_standard=?, malfunction=?, malfunction_url=?, repair_des=?,
                  remark=?, update_by=?, update_time=NOW(3)
                WHERE line_id=?
                """,
                DvJdbcHelper.longObj(body.get("repairId")),
                DvJdbcHelper.longObj(body.get("subjectId")),
                DvJdbcHelper.str(body.get("subjectCode")),
                DvJdbcHelper.str(body.get("subjectName")),
                DvJdbcHelper.str(body.get("subjectType")),
                DvJdbcHelper.str(body.get("subjectContent")),
                DvJdbcHelper.str(body.get("subjectStandard")),
                DvJdbcHelper.str(body.get("malfunction")),
                DvJdbcHelper.str(body.get("malfunctionUrl")),
                DvJdbcHelper.str(body.get("repairDes")),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("lineId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_repair_line WHERE line_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.eqLong(sql, args, "repair_id", params, "repairId");
    }
}
