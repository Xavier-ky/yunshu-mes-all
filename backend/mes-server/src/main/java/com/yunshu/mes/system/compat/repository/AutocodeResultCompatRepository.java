package com.yunshu.mes.system.compat.repository;

import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AutocodeResultCompatRepository {

    private final JdbcTemplate jdbc;

    public AutocodeResultCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findByRuleId(Long ruleId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT code_id, rule_id, gen_date, gen_index, last_result, last_serial_no, last_input_char
                FROM sys_auto_code_result WHERE rule_id = ? ORDER BY code_id DESC LIMIT 1
                """, (rs, n) -> mapResult(rs), ruleId);
        return rows.stream().findFirst();
    }

    public void insert(Long ruleId, String genDate, String lastResult, int genIndex, int lastSerialNo, String inputChar) {
        jdbc.update("""
                INSERT INTO sys_auto_code_result (rule_id, gen_date, gen_index, last_result, last_serial_no, last_input_char, create_by, create_time)
                VALUES (?,?,?,?,?,?,?,?)
                """,
                ruleId, genDate, genIndex, lastResult, lastSerialNo, inputChar == null ? "" : inputChar,
                SysCompatHelper.currentUsername(), SysCompatHelper.now());
    }

    public void update(Long codeId, String genDate, String lastResult, int genIndex, int lastSerialNo, String inputChar) {
        jdbc.update("""
                UPDATE sys_auto_code_result SET gen_date=?, gen_index=?, last_result=?, last_serial_no=?,
                    last_input_char=?, update_by=?, update_time=? WHERE code_id=?
                """,
                genDate, genIndex, lastResult, lastSerialNo, inputChar == null ? "" : inputChar,
                SysCompatHelper.currentUsername(), SysCompatHelper.now(), codeId);
    }

    private Map<String, Object> mapResult(ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codeId", rs.getLong("code_id"));
        m.put("ruleId", rs.getLong("rule_id"));
        m.put("genDate", rs.getString("gen_date"));
        m.put("genIndex", rs.getInt("gen_index"));
        m.put("lastResult", rs.getString("last_result"));
        m.put("lastSerialNo", rs.getObject("last_serial_no"));
        m.put("lastInputChar", rs.getString("last_input_char"));
        return m;
    }
}
