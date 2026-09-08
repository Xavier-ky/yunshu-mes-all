package com.yunshu.mes.integration.repository;

import com.yunshu.mes.integration.vo.SyncLogVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SyncLogRepository {

    private static final String SELECT_LIST = """
            SELECT sync_log_id, sync_type, biz_no, sync_status, sync_time
            FROM sync_log ORDER BY sync_time DESC
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public SyncLogRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<SyncLogVO> findAll() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query(SELECT_LIST, (rs, n) -> new SyncLogVO(
                rs.getLong("sync_log_id"),
                rs.getString("sync_type"),
                rs.getString("biz_no"),
                rs.getString("sync_status"),
                String.valueOf(rs.getTimestamp("sync_time"))));
    }

    public List<SyncLogVO> search(Map<String, String> params, int offset, int limit) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("""
                SELECT sync_log_id, sync_type, biz_no, sync_status, sync_time
                FROM sync_log WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("syncType"))) {
            sql.append(" AND sync_type LIKE ?");
            args.add("%" + params.get("syncType").trim() + "%");
        }
        if (StringUtils.hasText(params.get("bizNo"))) {
            sql.append(" AND biz_no LIKE ?");
            args.add("%" + params.get("bizNo").trim() + "%");
        }
        sql.append(" ORDER BY sync_time DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> new SyncLogVO(
                rs.getLong("sync_log_id"),
                rs.getString("sync_type"),
                rs.getString("biz_no"),
                rs.getString("sync_status"),
                String.valueOf(rs.getTimestamp("sync_time"))), args.toArray());
    }

    public long count(Map<String, String> params) {
        JdbcTemplate jdbc = requireJdbc();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sync_log WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("syncType"))) {
            sql.append(" AND sync_type LIKE ?");
            args.add("%" + params.get("syncType").trim() + "%");
        }
        if (StringUtils.hasText(params.get("bizNo"))) {
            sql.append(" AND biz_no LIKE ?");
            args.add("%" + params.get("bizNo").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            throw new DataAccessResourceFailureException("JdbcTemplate 未配置");
        }
        return jdbc;
    }
}
