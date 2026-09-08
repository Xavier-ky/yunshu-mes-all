package com.yunshu.mes.production.compat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** pro_andon_* row ↔ KTG API camelCase field mapping. */
public final class ProAndonSchemas {

    private ProAndonSchemas() {}

    public static Map<String, Object> mapRecord(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordId", rs.getLong("record_id"));
        m.put("workstationId", rs.getObject("workstation_id"));
        m.put("workstationCode", rs.getString("workstation_code"));
        m.put("workstationName", rs.getString("workstation_name"));
        m.put("userId", rs.getObject("user_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("nickName", rs.getString("nick_name"));
        m.put("workorderId", rs.getObject("workorder_id"));
        m.put("workorderCode", rs.getString("workorder_code"));
        m.put("workorderName", rs.getString("workorder_name"));
        m.put("processId", rs.getObject("process_id"));
        m.put("processCode", rs.getString("process_code"));
        m.put("processName", rs.getString("process_name"));
        m.put("andonReason", rs.getString("andon_reason"));
        m.put("andonLevel", rs.getString("andon_level"));
        m.put("handleTime", formatTs(rs.getTimestamp("handle_time")));
        m.put("handlerUserId", rs.getObject("handler_user_id"));
        m.put("handlerUserName", rs.getString("handler_user_name"));
        m.put("handlerNickName", rs.getString("handler_nick_name"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("attr1", rs.getString("attr1"));
        m.put("attr2", rs.getString("attr2"));
        m.put("attr3", rs.getObject("attr3"));
        m.put("attr4", rs.getObject("attr4"));
        m.put("createBy", rs.getString("create_by"));
        m.put("createTime", formatTs(rs.getTimestamp("create_time")));
        m.put("updateBy", rs.getString("update_by"));
        m.put("updateTime", formatTs(rs.getTimestamp("update_time")));
        return m;
    }

    public static Map<String, Object> mapConfig(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configId", rs.getLong("config_id"));
        m.put("andonReason", rs.getString("andon_reason"));
        m.put("andonLevel", rs.getString("andon_level"));
        m.put("handlerRoleId", rs.getObject("handler_role_id"));
        m.put("handlerRoleName", rs.getString("handler_role_name"));
        m.put("handlerUserId", rs.getObject("handler_user_id"));
        m.put("handlerUserName", rs.getString("handler_user_name"));
        m.put("handlerNickName", rs.getString("handler_nick_name"));
        m.put("remark", rs.getString("remark"));
        m.put("attr1", rs.getString("attr1"));
        m.put("attr2", rs.getString("attr2"));
        m.put("attr3", rs.getObject("attr3"));
        m.put("attr4", rs.getObject("attr4"));
        m.put("createBy", rs.getString("create_by"));
        m.put("createTime", formatTs(rs.getTimestamp("create_time")));
        m.put("updateBy", rs.getString("update_by"));
        m.put("updateTime", formatTs(rs.getTimestamp("update_time")));
        return m;
    }

    private static String formatTs(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        LocalDateTime ldt = ts.toLocalDateTime();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    }
}
