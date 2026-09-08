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
public class DvCheckRecordLineRepository {

    private static final String BASE = """
            SELECT line_id, record_id, subject_id, subject_code, subject_name, subject_type,
                   subject_content, subject_standard, check_status, check_result,
                   attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time, remark
            FROM dv_check_record_line
            """;

    private final JdbcTemplate jdbc;

    public DvCheckRecordLineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY line_id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapCheckRecordLine(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_check_record_line WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE line_id = ?",
                (rs, n) -> DvSchemas.mapCheckRecordLine(rs), id);
        return rows.stream().findFirst();
    }

    public int deleteByRecordId(Long recordId) {
        return jdbc.update("DELETE FROM dv_check_record_line WHERE record_id = ?", recordId);
    }

    public void insertFromPlanSubjects(Long recordId, List<Map<String, Object>> subjects) {
        for (Map<String, Object> s : subjects) {
            jdbc.update("""
                    INSERT INTO dv_check_record_line (
                      record_id, subject_id, subject_code, subject_name, subject_type,
                      subject_content, subject_standard, check_status, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,NOW(3))
                    """,
                    recordId,
                    DvJdbcHelper.longObj(s.get("subjectId")),
                    DvJdbcHelper.str(s.get("subjectCode")),
                    DvJdbcHelper.str(s.get("subjectName")),
                    DvJdbcHelper.str(s.get("subjectType")),
                    DvJdbcHelper.str(s.get("subjectContent")),
                    DvJdbcHelper.str(s.get("subjectStandard")),
                    "Y");
        }
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_check_record_line (
                      record_id, subject_id, subject_code, subject_name, subject_type,
                      subject_content, subject_standard, check_status, check_result, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("recordId")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("subjectId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectType")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectContent")));
            ps.setString(i++, DvJdbcHelper.str(body.get("subjectStandard")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("checkStatus"), "Y"));
            ps.setString(i++, DvJdbcHelper.str(body.get("checkResult")));
            ps.setTimestamp(i, DvJdbcHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        return jdbc.update("""
                UPDATE dv_check_record_line SET
                  record_id=?, subject_id=?, subject_code=?, subject_name=?, subject_type=?,
                  subject_content=?, subject_standard=?, check_status=?, check_result=?,
                  update_by=?, update_time=NOW(3)
                WHERE line_id=?
                """,
                DvJdbcHelper.longObj(body.get("recordId")),
                DvJdbcHelper.longObj(body.get("subjectId")),
                DvJdbcHelper.str(body.get("subjectCode")),
                DvJdbcHelper.str(body.get("subjectName")),
                DvJdbcHelper.str(body.get("subjectType")),
                DvJdbcHelper.str(body.get("subjectContent")),
                DvJdbcHelper.str(body.get("subjectStandard")),
                DvJdbcHelper.str(body.get("checkStatus")),
                DvJdbcHelper.str(body.get("checkResult")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("lineId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_check_record_line WHERE line_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.eqLong(sql, args, "record_id", params, "recordId");
        DvJdbcHelper.eqLong(sql, args, "subject_id", params, "subjectId");
    }
}
