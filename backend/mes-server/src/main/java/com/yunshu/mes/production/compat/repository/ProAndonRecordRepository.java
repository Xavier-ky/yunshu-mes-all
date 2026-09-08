package com.yunshu.mes.production.compat.repository;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.production.compat.ProAndonSchemas;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class ProAndonRecordRepository {

    private static final String BASE = """
            SELECT record_id, workstation_id, workstation_code, workstation_name,
                   user_id, user_name, nick_name,
                   workorder_id, workorder_code, workorder_name,
                   process_id, process_code, process_name,
                   andon_reason, andon_level, handle_time,
                   handler_user_id, handler_user_name, handler_nick_name,
                   status, remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM pro_andon_record
            """;

    private final JdbcTemplate jdbc;

    public ProAndonRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> ProAndonSchemas.mapRecord(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pro_andon_record WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE record_id = ?",
                (rs, n) -> ProAndonSchemas.mapRecord(rs), id);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO pro_andon_record (
                      workstation_id, workstation_code, workstation_name,
                      user_id, user_name, nick_name,
                      workorder_id, workorder_code, workorder_name,
                      process_id, process_code, process_name,
                      andon_reason, andon_level, handle_time,
                      handler_user_id, handler_user_name, handler_nick_name,
                      status, remark, attr1, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setObject(i++, body.get("workstationId"));
            ps.setString(i++, str(body.get("workstationCode")));
            ps.setString(i++, str(body.get("workstationName")));
            ps.setObject(i++, body.get("userId"));
            ps.setString(i++, str(body.get("userName")));
            ps.setString(i++, str(body.get("nickName")));
            ps.setObject(i++, body.get("workorderId"));
            ps.setString(i++, str(body.get("workorderCode")));
            ps.setString(i++, str(body.get("workorderName")));
            ps.setObject(i++, body.get("processId"));
            ps.setString(i++, str(body.get("processCode")));
            ps.setString(i++, str(body.get("processName")));
            ps.setString(i++, str(body.get("andonReason")));
            ps.setString(i++, strOr(body.get("andonLevel"), "LEVEL3"));
            ps.setTimestamp(i++, parseTs(body.get("handleTime")));
            ps.setObject(i++, body.get("handlerUserId"));
            ps.setString(i++, str(body.get("handlerUserName")));
            ps.setString(i++, str(body.get("handlerNickName")));
            ps.setString(i++, strOr(body.get("status"), "ACTIVE"));
            ps.setString(i++, str(body.get("remark")));
            ps.setString(i++, str(body.get("attr1")));
            ps.setString(i++, str(body.get("createBy")));
            ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        return jdbc.update("""
                UPDATE pro_andon_record SET
                  workstation_id=?, workstation_code=?, workstation_name=?,
                  user_id=?, user_name=?, nick_name=?,
                  workorder_id=?, workorder_code=?, workorder_name=?,
                  process_id=?, process_code=?, process_name=?,
                  andon_reason=?, andon_level=?, handle_time=?,
                  handler_user_id=?, handler_user_name=?, handler_nick_name=?,
                  status=?, remark=?, attr1=?, update_by=?, update_time=NOW(3)
                WHERE record_id=?
                """,
                body.get("workstationId"), str(body.get("workstationCode")), str(body.get("workstationName")),
                body.get("userId"), str(body.get("userName")), str(body.get("nickName")),
                body.get("workorderId"), str(body.get("workorderCode")), str(body.get("workorderName")),
                body.get("processId"), str(body.get("processCode")), str(body.get("processName")),
                str(body.get("andonReason")), str(body.get("andonLevel")), parseTs(body.get("handleTime")),
                body.get("handlerUserId"), str(body.get("handlerUserName")), str(body.get("handlerNickName")),
                str(body.get("status")), str(body.get("remark")), str(body.get("attr1")),
                str(body.get("updateBy")), body.get("recordId"));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM pro_andon_record WHERE record_id IN (" + placeholders + ")", ids.toArray());
    }

    public void updateBridgeEventId(Long recordId, Long eventId) {
        jdbc.update("UPDATE pro_andon_record SET attr1 = ? WHERE record_id = ?", String.valueOf(eventId), recordId);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        like(sql, args, "workstation_code", params.get("workstationCode"));
        like(sql, args, "workstation_name", params.get("workstationName"));
        like(sql, args, "nick_name", params.get("nickName"));
        eq(sql, args, "workorder_code", params.get("workorderCode"));
        like(sql, args, "process_name", params.get("processName"));
        like(sql, args, "handler_nick_name", params.get("handlerNickName"));
        eq(sql, args, "status", params.get("status"));
    }

    private static void like(StringBuilder sql, List<Object> args, String col, String val) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" LIKE ?");
            args.add("%" + val.trim() + "%");
        }
    }

    private static void eq(StringBuilder sql, List<Object> args, String col, String val) {
        if (StringUtils.hasText(val)) {
            sql.append(" AND ").append(col).append(" = ?");
            args.add(val.trim());
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Object v, String def) {
        String s = str(v);
        return (s == null || s.isBlank()) ? def : s;
    }

    private static Timestamp parseTs(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        String s = String.valueOf(v).trim().replace("T", " ");
        if (s.length() == 10) {
            s += " 00:00:00";
        }
        try {
            return Timestamp.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
