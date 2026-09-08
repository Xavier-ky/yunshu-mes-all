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
public class DvSubjectRepository {

    private static final String BASE = """
            SELECT subject_id, subject_code, subject_name, subject_type, subject_content, subject_standard,
                   enable_flag, remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_subject
            """;

    private final JdbcTemplate jdbc;

    public DvSubjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY subject_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapSubject(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_subject WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE subject_id = ?",
                (rs, n) -> DvSchemas.mapSubject(rs), id);
        return rows.stream().findFirst();
    }

    public boolean codeExists(String code, Long excludeId) {
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_subject WHERE subject_code = ?",
                    Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dv_subject WHERE subject_code = ? AND subject_id <> ?",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_subject (
                      subject_code, subject_name, subject_type, subject_content, subject_standard,
                      enable_flag, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectName")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("subjectType"), "CHECK"));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectContent")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectStandard")));
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
                UPDATE dv_subject SET
                  subject_code=?, subject_name=?, subject_type=?, subject_content=?, subject_standard=?,
                  enable_flag=?, remark=?, update_by=?, update_time=NOW(3)
                WHERE subject_id=?
                """,
                DvJdbcHelper.str(body.get("subjectCode")),
                DvJdbcHelper.str(body.get("subjectName")),
                DvJdbcHelper.str(body.get("subjectType")),
                DvJdbcHelper.str(body.get("subjectContent")),
                DvJdbcHelper.str(body.get("subjectStandard")),
                DvJdbcHelper.strOr(body.get("enableFlag"), "Y"),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("subjectId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_subject WHERE subject_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.like(sql, args, "subject_code", params.get("subjectCode"));
        DvJdbcHelper.like(sql, args, "subject_name", params.get("subjectName"));
        DvJdbcHelper.eq(sql, args, "subject_type", params.get("subjectType"));
        DvJdbcHelper.eq(sql, args, "enable_flag", params.get("enableFlag"));
    }
}
