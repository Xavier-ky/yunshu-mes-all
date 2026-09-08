package com.yunshu.mes.agent.repository;

import com.yunshu.mes.agent.dto.ChatMessage;
import com.yunshu.mes.agent.vo.ChatMessageVO;
import com.yunshu.mes.agent.vo.ChatSessionVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * AI 对话会话/消息持久化，基于 JdbcTemplate。
 */
@Repository
public class AiChatRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public AiChatRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public SessionRow findSessionByUuid(String sessionUuid) {
        JdbcTemplate jdbc = requireJdbc();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, session_uuid, title FROM ai_chat_session WHERE session_uuid = ? LIMIT 1", sessionUuid);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        return new SessionRow(longValue(r.get("id")), stringValue(r.get("session_uuid")), stringValue(r.get("title")));
    }

    public Long createSession(String sessionUuid, String title) {
        JdbcTemplate jdbc = requireJdbc();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO ai_chat_session (session_uuid, title, last_message_at) VALUES (?, ?, NOW(3))",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sessionUuid);
            ps.setString(2, title);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void touchSession(Long sessionId) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE ai_chat_session SET last_message_at = NOW(3) WHERE id = ?", sessionId);
    }

    public void updateSessionTitle(Long sessionId, String title) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("UPDATE ai_chat_session SET title = ? WHERE id = ?", title, sessionId);
    }

    public boolean deleteSession(String sessionUuid) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.update("DELETE FROM ai_chat_session WHERE session_uuid = ?", sessionUuid) > 0;
    }

    public List<ChatSessionVO> listSessions(int limit) {
        JdbcTemplate jdbc = requireJdbc();
        String sql = """
                SELECT s.session_uuid, s.title,
                       DATE_FORMAT(s.created_at, '%Y-%m-%d %H:%i:%s') AS created_at,
                       DATE_FORMAT(s.last_message_at, '%Y-%m-%d %H:%i:%s') AS last_message_at,
                       (SELECT m.content FROM ai_chat_message m WHERE m.session_id = s.id ORDER BY m.id DESC LIMIT 1) AS latest_message
                FROM ai_chat_session s
                ORDER BY COALESCE(s.last_message_at, s.created_at) DESC
                LIMIT ?
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Math.max(limit, 1));
        List<ChatSessionVO> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String title = stringValue(r.get("title"));
            String latest = stringValue(r.get("latest_message"));
            list.add(new ChatSessionVO(
                    stringValue(r.get("session_uuid")),
                    title,
                    title,
                    stringValue(r.get("created_at")),
                    stringValue(r.get("last_message_at")),
                    latest == null ? "" : latest.substring(0, Math.min(latest.length(), 100))));
        }
        return list;
    }

    public List<ChatMessageVO> listMessages(Long sessionId, int limit) {
        JdbcTemplate jdbc = requireJdbc();
        String sql = """
                SELECT role, content, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS time
                FROM ai_chat_message
                WHERE session_id = ?
                ORDER BY id ASC
                LIMIT ?
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, sessionId, Math.max(limit, 1));
        List<ChatMessageVO> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            list.add(new ChatMessageVO(stringValue(r.get("role")), stringValue(r.get("content")), stringValue(r.get("time"))));
        }
        return list;
    }

    public List<ChatMessage> loadHistory(Long sessionId, int turns) {
        JdbcTemplate jdbc = requireJdbc();
        int limit = Math.max(turns * 2, 2);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT role, content FROM ai_chat_message WHERE session_id = ? AND role IN ('user','assistant') ORDER BY id DESC LIMIT ?",
                sessionId, limit);
        List<ChatMessage> history = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            history.add(new ChatMessage(stringValue(r.get("role")), stringValue(r.get("content"))));
        }
        Collections.reverse(history);
        return history;
    }

    public void insertMessage(Long sessionId, String role, String content, String modelName, Integer tokenCount, Integer latencyMs) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("INSERT INTO ai_chat_message (message_uuid, session_id, role, content, model_name, token_count, latency_ms) "
                + "VALUES (UUID(), ?, ?, ?, ?, ?, ?)",
                sessionId, role, content, modelName, tokenCount, latencyMs);
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        }
        return jdbc;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public record SessionRow(Long id, String uuid, String title) {
    }
}
