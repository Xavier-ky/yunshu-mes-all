package com.yunshu.mes.reporting.compat.repository;

import com.yunshu.mes.reporting.compat.AnalyticsJdbcHelper;
import com.yunshu.mes.system.compat.SysCompatHelper;
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
public class UreportCompatRepository {

    private static final String BASE = """
            SELECT id_, name_, create_time_, update_time_
            FROM ureport_file_tbl
            """;

    private final JdbcTemplate jdbc;

    public UreportCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("name"))) {
            sql.append(" AND name_ LIKE ?");
            args.add("%" + params.get("name").trim() + "%");
        }
        sql.append(" ORDER BY id_ DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::ureportRow, args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ureport_file_tbl WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("name"))) {
            sql.append(" AND name_ LIKE ?");
            args.add("%" + params.get("name").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id_, name_, content_, create_time_, update_time_
                FROM ureport_file_tbl WHERE id_ = ?
                """, AnalyticsJdbcHelper::ureportDetailRow, id);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String name = SysCompatHelper.str(body.get("name"));
        String content = SysCompatHelper.str(body.get("content"));
        if (!StringUtils.hasText(content)) {
            content = peekContentByName("MES通用表格模板.ureport.xml");
            if (!StringUtils.hasText(content)) {
                content = MINIMAL_UREPORT_XML;
            }
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("报表名称不能为空");
        }
        if (!name.endsWith(".ureport.xml")) {
            name = name + ".ureport.xml";
        }
        final String finalName = name;
        final String finalContent = content;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO ureport_file_tbl (name_, content_, create_time_, update_time_) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, finalName);
            ps.setString(2, finalContent);
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    private String peekContentByName(String name) {
        List<String> rows = jdbc.query(
                "SELECT content_ FROM ureport_file_tbl WHERE name_ = ?",
                (rs, n) -> rs.getString("content_"),
                name);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private static final String MINIMAL_UREPORT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ureport>
              <cell expand="None" name="A1" row="1" col="1"><cell-style font-size="12" align="center" valign="middle"/><simple-value><![CDATA[新报表]]></simple-value></cell>
              <row row-number="1" height="30"/>
              <column col-number="1" width="120"/>
              <paper type="A4" left-margin="90" right-margin="90"
                top-margin="72" bottom-margin="72" paging-mode="fitpage" fixrows="0"
                width="595" height="842" orientation="portrait" html-report-align="left" bg-image="" html-interval-refresh-value="0" column-enabled="false"/>
            </ureport>
            """;

    public int update(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update(
                "UPDATE ureport_file_tbl SET name_ = ?, content_ = ?, update_time_ = ? WHERE id_ = ?",
                SysCompatHelper.str(body.get("name")),
                SysCompatHelper.str(body.get("content")),
                now,
                SysCompatHelper.longVal(body.get("id")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM ureport_file_tbl WHERE id_ IN (" + placeholders + ")", ids.toArray());
    }

    public List<String> findNamesByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.queryForList(
                "SELECT name_ FROM ureport_file_tbl WHERE id_ IN (" + placeholders + ")",
                String.class,
                ids.toArray());
    }

    public Optional<String> findContentByName(String name) {
        List<String> rows = jdbc.query(
                "SELECT content_ FROM ureport_file_tbl WHERE name_ = ?",
                (rs, n) -> rs.getString("content_"),
                name);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        String content = rows.get(0);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("报表内容为空，请重新安装模板或保存设计: " + name);
        }
        return Optional.of(content);
    }

    /** 导出档案列表（名称 / 创建 / 更新时间），不含 XML 正文。 */
    public List<Map<String, Object>> listForExport(Map<String, String> params) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("name"))) {
            sql.append(" AND name_ LIKE ?");
            args.add("%" + params.get("name").trim() + "%");
        }
        sql.append(" ORDER BY id_ DESC");
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::ureportRow, args.toArray());
    }

    public List<ReportFileRow> listReportFiles() {
        return jdbc.query(
                "SELECT name_, update_time_ FROM ureport_file_tbl ORDER BY update_time_ DESC",
                (rs, n) -> new ReportFileRow(
                        rs.getString("name_"),
                        rs.getTimestamp("update_time_") == null
                                ? System.currentTimeMillis()
                                : rs.getTimestamp("update_time_").getTime()));
    }

    public void saveByName(String name, String content) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int updated = jdbc.update(
                "UPDATE ureport_file_tbl SET content_ = ?, update_time_ = ? WHERE name_ = ?",
                content, now, name);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO ureport_file_tbl (name_, content_, create_time_, update_time_) VALUES (?,?,?,?)",
                    name, content, now, now);
        }
    }

    public void deleteByName(String name) {
        jdbc.update("DELETE FROM ureport_file_tbl WHERE name_ = ?", name);
    }

    public record ReportFileRow(String name, long updateTime) {}
}
