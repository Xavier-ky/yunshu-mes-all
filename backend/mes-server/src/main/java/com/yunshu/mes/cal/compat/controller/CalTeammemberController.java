package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/cal/teammember")
public class CalTeammemberController {

    private final JdbcTemplate jdbc;

    public CalTeammemberController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.table(List.of(), 0);
        }
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        String teamId = params.get("teamId");
        List<Object> args = new ArrayList<>();
        String sql = """
                SELECT member_id, team_id, user_id, user_name, nick_name, tel, remark
                FROM cal_team_member
                """;
        String countSql = "SELECT COUNT(*) FROM cal_team_member";
        if (teamId != null && !teamId.isBlank()) {
            sql += " WHERE team_id = ?";
            countSql += " WHERE team_id = ?";
            args.add(Long.parseLong(teamId));
        }
        sql += " ORDER BY member_id LIMIT ? OFFSET ?";
        List<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(ps);
        queryArgs.add(PageUtil.offset(pn, ps));
        List<Map<String, Object>> rows = jdbc.query(sql, (rs, n) -> mapMember(rs), queryArgs.toArray());
        Long total = args.isEmpty()
                ? jdbc.queryForObject(countSql, Long.class)
                : jdbc.queryForObject(countSql, Long.class, args.toArray());
        return MesApiResponse.table(rows, total == null ? 0 : total);
    }

    @GetMapping("/getListByTeamId")
    public Map<String, Object> getListByTeamId(@RequestParam(required = false) String ids) {
        if (ids == null || ids.isBlank()) {
            return MesApiResponse.ok(List.of());
        }
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.ok(List.of());
        }
        List<Long> teamIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        String placeholders = teamIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT member_id, team_id, user_id, user_name, nick_name, tel, remark
                FROM cal_team_member WHERE team_id IN (""" + placeholders + ") ORDER BY team_id, member_id",
                (rs, n) -> mapMember(rs), teamIds.toArray());
        return MesApiResponse.ok(rows);
    }

    @GetMapping("/{memberId}")
    public Map<String, Object> getInfo(@PathVariable Long memberId) {
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.error("成员不存在");
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT member_id, team_id, user_id, user_name, nick_name, tel, remark
                FROM cal_team_member WHERE member_id = ?
                """, (rs, n) -> mapMember(rs), memberId);
        return rows.isEmpty() ? MesApiResponse.error("成员不存在") : MesApiResponse.ok(rows.get(0));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.ok(1L);
        }
        jdbc.update("""
                INSERT INTO cal_team_member (team_id, user_id, user_name, nick_name, tel, remark, create_time)
                VALUES (?, ?, ?, ?, ?, ?, NOW(3))
                """, longVal(body, "teamId"), longVal(body, "userId"),
                str(body, "userName"), str(body, "nickName"), str(body, "tel", ""),
                str(body, "remark", ""));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.toAjax(1);
        }
        int n = jdbc.update("""
                UPDATE cal_team_member SET team_id=?, user_id=?, user_name=?, nick_name=?, tel=?, remark=?,
                  update_time=NOW(3)
                WHERE member_id=?
                """, longVal(body, "teamId"), longVal(body, "userId"),
                str(body, "userName"), str(body, "nickName"), str(body, "tel", ""),
                str(body, "remark", ""), longVal(body, "memberId"));
        return MesApiResponse.toAjax(n);
    }

    @DeleteMapping("/{memberIds}")
    public Map<String, Object> remove(@PathVariable String memberIds) {
        if (!tableExists("cal_team_member")) {
            return MesApiResponse.toAjax(1);
        }
        int n = 0;
        for (String p : memberIds.split(",")) {
            n += jdbc.update("DELETE FROM cal_team_member WHERE member_id=?", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private Map<String, Object> mapMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("memberId", rs.getLong("member_id"));
        m.put("teamId", rs.getLong("team_id"));
        m.put("userId", rs.getLong("user_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("nickName", rs.getString("nick_name"));
        m.put("tel", rs.getString("tel"));
        m.put("remark", rs.getString("remark"));
        return m;
    }

    private boolean tableExists(String table) {
        try {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = ?
                    """, Integer.class, table);
            return count != null && count > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private static String str(Map<String, Object> m, String key) {
        return str(m, key, null);
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : String.valueOf(v);
    }

    private static long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
