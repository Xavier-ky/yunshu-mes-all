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
import org.springframework.util.StringUtils;

@Repository
public class MenuCompatRepository {

    private static final String BASE = """
            SELECT menu_id, menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                   menu_type, visible, status, perms, icon, remark, create_time, update_time
            FROM sys_menu
            """;

    private final JdbcTemplate jdbc;

    public MenuCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> selectList(Map<String, String> params) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("menuName"))) {
            SysCompatHelper.like(sql, args, "menu_name", params.get("menuName"));
        }
        if (StringUtils.hasText(params.get("status"))) {
            SysCompatHelper.eq(sql, args, "status", params.get("status"));
        }
        sql.append(" ORDER BY parent_id, order_num, menu_id");
        return jdbc.query(sql.toString(), (rs, n) -> mapMenu(rs), args.toArray());
    }

    public Optional<Map<String, Object>> findById(Long menuId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE menu_id = ?", (rs, n) -> mapMenu(rs), menuId);
        return rows.stream().findFirst();
    }

    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return jdbc.query("SELECT menu_id FROM sys_role_menu WHERE role_id = ?", (rs, n) -> rs.getLong(1), roleId);
    }

    public boolean menuNameExists(String menuName, Long parentId, Long excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM sys_menu WHERE menu_name = ? AND parent_id = ?"
                : "SELECT COUNT(*) FROM sys_menu WHERE menu_name = ? AND parent_id = ? AND menu_id <> ?";
        Long c = excludeId == null
                ? jdbc.queryForObject(sql, Long.class, menuName, parentId == null ? 0L : parentId)
                : jdbc.queryForObject(sql, Long.class, menuName, parentId == null ? 0L : parentId, excludeId);
        return c != null && c > 0;
    }

    public boolean hasChildren(Long menuId) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE parent_id = ?", Long.class, menuId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, is_frame, is_cache,
                        menu_type, visible, status, perms, icon, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("menuName")));
            ps.setLong(i++, SysCompatHelper.longObj(body.get("parentId")) == null ? 0L : SysCompatHelper.longObj(body.get("parentId")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("orderNum")) == null ? 0 : SysCompatHelper.intObj(body.get("orderNum")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("path"), ""));
            ps.setString(i++, SysCompatHelper.str(body.get("component")));
            ps.setString(i++, SysCompatHelper.str(body.get("query")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("isFrame")) == null ? 1 : SysCompatHelper.intObj(body.get("isFrame")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("isCache")) == null ? 0 : SysCompatHelper.intObj(body.get("isCache")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("menuType"), "M"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("visible"), "0"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "0"));
            ps.setString(i++, SysCompatHelper.str(body.get("perms")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("icon"), "#"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("remark"), ""));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long menuId = SysCompatHelper.longObj(body.get("menuId"));
        if (menuId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_menu SET menu_name=?, parent_id=?, order_num=?, path=?, component=?, query=?,
                    is_frame=?, is_cache=?, menu_type=?, visible=?, status=?, perms=?, icon=?, remark=?,
                    update_by=?, update_time=?
                WHERE menu_id=?
                """,
                SysCompatHelper.str(body.get("menuName")),
                SysCompatHelper.longObj(body.get("parentId")) == null ? 0L : SysCompatHelper.longObj(body.get("parentId")),
                SysCompatHelper.intObj(body.get("orderNum")) == null ? 0 : SysCompatHelper.intObj(body.get("orderNum")),
                SysCompatHelper.strOr(body.get("path"), ""),
                SysCompatHelper.str(body.get("component")),
                SysCompatHelper.str(body.get("query")),
                SysCompatHelper.intObj(body.get("isFrame")) == null ? 1 : SysCompatHelper.intObj(body.get("isFrame")),
                SysCompatHelper.intObj(body.get("isCache")) == null ? 0 : SysCompatHelper.intObj(body.get("isCache")),
                SysCompatHelper.strOr(body.get("menuType"), "M"),
                SysCompatHelper.strOr(body.get("visible"), "0"),
                SysCompatHelper.strOr(body.get("status"), "0"),
                SysCompatHelper.str(body.get("perms")),
                SysCompatHelper.strOr(body.get("icon"), "#"),
                SysCompatHelper.strOr(body.get("remark"), ""),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                menuId);
    }

    public int deleteById(Long menuId) {
        jdbc.update("DELETE FROM sys_role_menu WHERE menu_id = ?", menuId);
        return jdbc.update("DELETE FROM sys_menu WHERE menu_id = ?", menuId);
    }

    private Map<String, Object> mapMenu(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("menuId", rs.getLong("menu_id"));
        m.put("menuName", rs.getString("menu_name"));
        m.put("parentId", rs.getLong("parent_id"));
        m.put("orderNum", rs.getInt("order_num"));
        m.put("path", rs.getString("path"));
        m.put("component", rs.getString("component"));
        m.put("query", rs.getString("query"));
        m.put("isFrame", rs.getInt("is_frame"));
        m.put("isCache", rs.getInt("is_cache"));
        m.put("menuType", rs.getString("menu_type"));
        m.put("visible", rs.getString("visible"));
        m.put("status", rs.getString("status"));
        m.put("perms", rs.getString("perms"));
        m.put("icon", rs.getString("icon"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", SysCompatHelper.getTs(rs, "create_time"));
        return m;
    }
}
