package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NoticeCompatRepository {

    private static final String BASE = "SELECT notice_id, notice_title, notice_type, notice_content, status, remark, create_time FROM sys_notice";

    private final JdbcTemplate jdbc;

    public NoticeCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "notice_title", params.get("noticeTitle"));
        SysCompatHelper.eq(sql, args, "notice_type", params.get("noticeType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        sql.append(" ORDER BY notice_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapNotice(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_notice WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.like(sql, args, "notice_title", params.get("noticeTitle"));
        SysCompatHelper.eq(sql, args, "notice_type", params.get("noticeType"));
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Integer noticeId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE notice_id = ?", (rs, n) -> mapNotice(rs), noticeId);
        return rows.stream().findFirst();
    }

    public Integer insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sys_notice (notice_title, notice_type, notice_content, status, remark, create_by, create_time) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("noticeTitle")));
            ps.setString(i++, SysCompatHelper.str(body.get("noticeType")));
            byte[] content = SysCompatHelper.str(body.get("noticeContent")) == null
                    ? new byte[0] : SysCompatHelper.str(body.get("noticeContent")).getBytes();
            ps.setBytes(i++, content);
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "0"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.intValue();
    }

    public int update(Map<String, Object> body) {
        Integer noticeId = SysCompatHelper.intObj(body.get("noticeId"));
        if (noticeId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_notice SET notice_title=?, notice_type=?, notice_content=?, status=?, remark=?,
                    update_by=?, update_time=?
                WHERE notice_id=?
                """,
                SysCompatHelper.str(body.get("noticeTitle")),
                SysCompatHelper.str(body.get("noticeType")),
                SysCompatHelper.str(body.get("noticeContent")) == null
                        ? new byte[0] : SysCompatHelper.str(body.get("noticeContent")).getBytes(),
                SysCompatHelper.strOr(body.get("status"), "0"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                noticeId);
    }

    public int deleteByIds(List<Integer> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_notice WHERE notice_id IN (" + ph + ")", ids.toArray());
    }

    private Map<String, Object> mapNotice(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("noticeId", rs.getInt("notice_id"));
        m.put("noticeTitle", rs.getString("notice_title"));
        m.put("noticeType", rs.getString("notice_type"));
        byte[] content = rs.getBytes("notice_content");
        m.put("noticeContent", content == null ? "" : new String(content));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
