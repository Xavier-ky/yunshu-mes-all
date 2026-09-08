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
public class AutocodePartCompatRepository {

    private static final String BASE = """
            SELECT part_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format,
                   input_character, fix_character, seria_start_no, seria_step, seria_now_no, cycle_flag, cycle_method, remark
            FROM sys_auto_code_part
            """;

    private final JdbcTemplate jdbc;

    public AutocodePartCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listByRuleId(Long ruleId) {
        return jdbc.query(BASE + " WHERE rule_id = ? ORDER BY part_index", (rs, n) -> mapPart(rs), ruleId);
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.eqLong(sql, args, "rule_id", params, "ruleId");
        sql.append(" ORDER BY rule_id, part_index LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapPart(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sys_auto_code_part WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        SysCompatHelper.eqLong(sql, args, "rule_id", params, "ruleId");
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findById(Long partId) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE part_id = ?", (rs, n) -> mapPart(rs), partId);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO sys_auto_code_part (rule_id, part_index, part_type, part_code, part_name, part_length,
                        date_format, input_character, fix_character, seria_start_no, seria_step, seria_now_no,
                        cycle_flag, cycle_method, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, SysCompatHelper.longVal(body.get("ruleId")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("partIndex")) == null ? 1 : SysCompatHelper.intObj(body.get("partIndex")));
            ps.setString(i++, SysCompatHelper.str(body.get("partType")));
            ps.setString(i++, SysCompatHelper.str(body.get("partCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("partName")));
            ps.setInt(i++, SysCompatHelper.intObj(body.get("partLength")) == null ? 1 : SysCompatHelper.intObj(body.get("partLength")));
            ps.setString(i++, SysCompatHelper.str(body.get("dateFormat")));
            ps.setString(i++, SysCompatHelper.str(body.get("inputCharacter")));
            ps.setString(i++, SysCompatHelper.str(body.get("fixCharacter")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("seriaStartNo")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("seriaStep")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("seriaNowNo")));
            ps.setString(i++, SysCompatHelper.str(body.get("cycleFlag")));
            ps.setString(i++, SysCompatHelper.str(body.get("cycleMethod")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("remark"), ""));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, SysCompatHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        Long partId = SysCompatHelper.longObj(body.get("partId"));
        if (partId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE sys_auto_code_part SET rule_id=?, part_index=?, part_type=?, part_code=?, part_name=?,
                    part_length=?, date_format=?, input_character=?, fix_character=?, seria_start_no=?, seria_step=?,
                    seria_now_no=?, cycle_flag=?, cycle_method=?, remark=?, update_by=?, update_time=?
                WHERE part_id=?
                """,
                SysCompatHelper.longVal(body.get("ruleId")),
                SysCompatHelper.intObj(body.get("partIndex")) == null ? 1 : SysCompatHelper.intObj(body.get("partIndex")),
                SysCompatHelper.str(body.get("partType")),
                SysCompatHelper.str(body.get("partCode")),
                SysCompatHelper.str(body.get("partName")),
                SysCompatHelper.intObj(body.get("partLength")) == null ? 1 : SysCompatHelper.intObj(body.get("partLength")),
                SysCompatHelper.str(body.get("dateFormat")),
                SysCompatHelper.str(body.get("inputCharacter")),
                SysCompatHelper.str(body.get("fixCharacter")),
                SysCompatHelper.intObj(body.get("seriaStartNo")),
                SysCompatHelper.intObj(body.get("seriaStep")),
                SysCompatHelper.intObj(body.get("seriaNowNo")),
                SysCompatHelper.str(body.get("cycleFlag")),
                SysCompatHelper.str(body.get("cycleMethod")),
                SysCompatHelper.strOr(body.get("remark"), ""),
                SysCompatHelper.currentUsername(),
                SysCompatHelper.now(),
                partId);
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = ids.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        return jdbc.update("DELETE FROM sys_auto_code_part WHERE part_id IN (" + ph + ")", ids.toArray());
    }

    public int updateSerialNow(Long partId, int nowNo) {
        return jdbc.update("UPDATE sys_auto_code_part SET seria_now_no = ? WHERE part_id = ?", nowNo, partId);
    }

    private Map<String, Object> mapPart(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("partId", rs.getLong("part_id"));
        m.put("ruleId", rs.getLong("rule_id"));
        m.put("partIndex", rs.getInt("part_index"));
        m.put("partType", rs.getString("part_type"));
        m.put("partCode", rs.getString("part_code"));
        m.put("partName", rs.getString("part_name"));
        m.put("partLength", rs.getInt("part_length"));
        m.put("dateFormat", rs.getString("date_format"));
        m.put("inputCharacter", rs.getString("input_character"));
        m.put("fixCharacter", rs.getString("fix_character"));
        m.put("seriaStartNo", rs.getObject("seria_start_no"));
        m.put("seriaStep", rs.getObject("seria_step"));
        m.put("seriaNowNo", rs.getObject("seria_now_no"));
        m.put("cycleFlag", rs.getString("cycle_flag"));
        m.put("cycleMethod", rs.getString("cycle_method"));
        m.put("remark", rs.getString("remark"));
        return m;
    }
}
