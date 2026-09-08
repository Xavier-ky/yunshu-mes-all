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
public class MessageCompatRepository {

    private static final String BASE = """
            SELECT message_id, message_type, message_level, message_title, message_content,
                   sender_id, sender_name, recipient_id, recipient_name, process_time, status, deleted_flag, create_time
            FROM sys_message
            """;

    private final JdbcTemplate jdbc;

    public MessageCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE deleted_flag = 'N' ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.eq(sql, args, "message_type", params.get("messageType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        SysCompatHelper.eqLong(sql, args, "recipient_id", params, "recipientId");
        sql.append(" ORDER BY message_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapMessage(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_message WHERE deleted_flag = 'N' ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.eq(sql, args, "message_type", params.get("messageType"));
        SysCompatHelper.eq(sql, args, "status", params.get("status"));
        SysCompatHelper.eqLong(sql, args, "recipient_id", params, "recipientId");
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long messageId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE message_id = ? AND deleted_flag = 'N'",
                (rs, n) -> mapMessage(rs), messageId);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_message (message_type, message_level, message_title, message_content,
                        sender_id, sender_name, recipient_id, recipient_name, status, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("messageType")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("messageLevel"), "INFO"));
            ps.setString(i++, SysCompatHelper.str(body.get("messageTitle")));
            byte[] content = SysCompatHelper.str(body.get("messageContent")) == null
                    ? new byte[0] : SysCompatHelper.str(body.get("messageContent")).getBytes();
            ps.setBytes(i++, content);
            ps.setObject(i++, SysCompatHelper.longObj(body.get("senderId")));
            ps.setString(i++, SysCompatHelper.str(body.get("senderName")));
            ps.setLong(i++, SysCompatHelper.longVal(body.get("recipientId")));
            ps.setString(i++, SysCompatHelper.str(body.get("recipientName")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "UNREAD"));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long messageId = SysCompatHelper.longObj(body.get("messageId"));
        if (messageId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_message SET message_type=?, message_level=?, message_title=?, message_content=?,
                    status=?, process_time=?, update_by=?, update_time=?
                WHERE message_id=? AND deleted_flag='N'
                """,
                SysCompatHelper.str(body.get("messageType")),
                SysCompatHelper.str(body.get("messageLevel")),
                SysCompatHelper.str(body.get("messageTitle")),
                SysCompatHelper.str(body.get("messageContent")) == null
                        ? new byte[0] : SysCompatHelper.str(body.get("messageContent")).getBytes(),
                SysCompatHelper.str(body.get("status")),
                SysCompatHelper.now(),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                messageId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("UPDATE sys_message SET deleted_flag='Y', update_time=? WHERE message_id IN (" + ph + ")",
                prependNow(ids));
    }

    private Object[] prependNow(List<Long> ids) {
        List<Object> args = new ArrayList<>();
        args.add(SysCompatHelper.now());
        args.addAll(ids);
        return args.toArray();
    }

    private Map<String, Object> mapMessage(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("messageId", rs.getLong("message_id"));
        m.put("messageType", rs.getString("message_type"));
        m.put("messageLevel", rs.getString("message_level"));
        m.put("messageTitle", rs.getString("message_title"));
        byte[] content = rs.getBytes("message_content");
        m.put("messageContent", content == null ? "" : new String(content));
        m.put("senderId", rs.getObject("sender_id"));
        m.put("senderName", rs.getString("sender_name"));
        m.put("recipientId", rs.getLong("recipient_id"));
        m.put("recipientName", rs.getString("recipient_name"));
        m.put("processTime", SysCompatHelper.getTs(rs, "process_time"));
        m.put("status", rs.getString("status"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
