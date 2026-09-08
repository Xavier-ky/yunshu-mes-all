package com.yunshu.mes.equipment.compat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** dv_* row ↔ KTG API camelCase field mapping. */
public final class DvSchemas {

    private DvSchemas() {}

    public static Map<String, Object> mapMachineryType(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("machineryTypeId", rs.getLong("machinery_type_id"));
        m.put("machineryTypeCode", rs.getString("machinery_type_code"));
        m.put("machineryTypeName", rs.getString("machinery_type_name"));
        m.put("parentTypeId", rs.getObject("parent_type_id"));
        m.put("ancestors", rs.getString("ancestors"));
        m.put("enableFlag", rs.getString("enable_flag"));
        return m;
    }

    public static Map<String, Object> mapMachinery(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("machineryId", rs.getLong("machinery_id"));
        m.put("machineryCode", rs.getString("machinery_code"));
        m.put("machineryName", rs.getString("machinery_name"));
        m.put("machineryBrand", rs.getString("machinery_brand"));
        m.put("machinerySpec", rs.getString("machinery_spec"));
        m.put("machineryTypeId", rs.getObject("machinery_type_id"));
        m.put("machineryTypeCode", rs.getString("machinery_type_code"));
        m.put("machineryTypeName", rs.getString("machinery_type_name"));
        m.put("workshopId", rs.getObject("workshop_id"));
        m.put("workshopCode", rs.getString("workshop_code"));
        m.put("workshopName", rs.getString("workshop_name"));
        m.put("lastMaintenTime", formatTs(rs.getTimestamp("last_mainten_time")));
        m.put("lastCheckTime", formatTs(rs.getTimestamp("last_check_time")));
        m.put("status", rs.getString("status"));
        return m;
    }

    public static Map<String, Object> mapSubject(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("subjectId", rs.getLong("subject_id"));
        m.put("subjectCode", rs.getString("subject_code"));
        m.put("subjectName", rs.getString("subject_name"));
        m.put("subjectType", rs.getString("subject_type"));
        m.put("subjectContent", rs.getString("subject_content"));
        m.put("subjectStandard", rs.getString("subject_standard"));
        m.put("enableFlag", rs.getString("enable_flag"));
        return m;
    }

    public static Map<String, Object> mapCheckPlan(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("planId", rs.getLong("plan_id"));
        m.put("planCode", rs.getString("plan_code"));
        m.put("planName", rs.getString("plan_name"));
        m.put("planType", rs.getString("plan_type"));
        m.put("startDate", formatTs(rs.getTimestamp("start_date")));
        m.put("endDate", formatTs(rs.getTimestamp("end_date")));
        m.put("cycleType", rs.getString("cycle_type"));
        m.put("cycleCount", rs.getObject("cycle_count"));
        m.put("status", rs.getString("status"));
        return m;
    }

    public static Map<String, Object> mapCheckMachinery(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("recordId", rs.getLong("record_id"));
        m.put("planId", rs.getObject("plan_id"));
        m.put("machineryId", rs.getObject("machinery_id"));
        m.put("machineryCode", rs.getString("machinery_code"));
        m.put("machineryName", rs.getString("machinery_name"));
        m.put("machineryBrand", rs.getString("machinery_brand"));
        m.put("machinerySpec", rs.getString("machinery_spec"));
        return m;
    }

    public static Map<String, Object> mapCheckSubject(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("recordId", rs.getLong("record_id"));
        m.put("planId", rs.getObject("plan_id"));
        m.put("subjectId", rs.getObject("subject_id"));
        m.put("subjectCode", rs.getString("subject_code"));
        m.put("subjectName", rs.getString("subject_name"));
        m.put("subjectType", rs.getString("subject_type"));
        m.put("subjectContent", rs.getString("subject_content"));
        m.put("subjectStandard", rs.getString("subject_standard"));
        return m;
    }

    public static Map<String, Object> mapCheckRecord(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("recordId", rs.getLong("record_id"));
        m.put("planId", rs.getObject("plan_id"));
        m.put("planCode", rs.getString("plan_code"));
        m.put("planName", rs.getString("plan_name"));
        m.put("planType", rs.getString("plan_type"));
        m.put("machineryId", rs.getObject("machinery_id"));
        m.put("machineryCode", rs.getString("machinery_code"));
        m.put("machineryName", rs.getString("machinery_name"));
        m.put("machineryBrand", rs.getString("machinery_brand"));
        m.put("machinerySpec", rs.getString("machinery_spec"));
        m.put("checkTime", formatTs(rs.getTimestamp("check_time")));
        m.put("userId", rs.getObject("user_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("nickName", rs.getString("nick_name"));
        m.put("status", rs.getString("status"));
        return m;
    }

    public static Map<String, Object> mapCheckRecordLine(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("lineId", rs.getLong("line_id"));
        m.put("recordId", rs.getObject("record_id"));
        m.put("subjectId", rs.getObject("subject_id"));
        m.put("subjectCode", rs.getString("subject_code"));
        m.put("subjectName", rs.getString("subject_name"));
        m.put("subjectType", rs.getString("subject_type"));
        m.put("subjectContent", rs.getString("subject_content"));
        m.put("subjectStandard", rs.getString("subject_standard"));
        m.put("checkStatus", rs.getString("check_status"));
        m.put("checkResult", rs.getString("check_result"));
        return m;
    }

    public static Map<String, Object> mapMaintenRecord(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("recordId", rs.getLong("record_id"));
        m.put("planId", rs.getObject("plan_id"));
        m.put("planCode", rs.getString("plan_code"));
        m.put("planName", rs.getString("plan_name"));
        m.put("planType", rs.getString("plan_type"));
        m.put("machineryId", rs.getObject("machinery_id"));
        m.put("machineryCode", rs.getString("machinery_code"));
        m.put("machineryName", rs.getString("machinery_name"));
        m.put("machineryBrand", rs.getString("machinery_brand"));
        m.put("machinerySpec", rs.getString("machinery_spec"));
        m.put("maintenTime", formatTs(rs.getTimestamp("mainten_time")));
        m.put("userId", rs.getObject("user_id"));
        m.put("userName", rs.getString("user_name"));
        m.put("nickName", rs.getString("nick_name"));
        m.put("status", rs.getString("status"));
        return m;
    }

    public static Map<String, Object> mapMaintenRecordLine(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("lineId", rs.getLong("line_id"));
        m.put("recordId", rs.getObject("record_id"));
        m.put("subjectId", rs.getObject("subject_id"));
        m.put("subjectCode", rs.getString("subject_code"));
        m.put("subjectName", rs.getString("subject_name"));
        m.put("subjectType", rs.getString("subject_type"));
        m.put("subjectContent", rs.getString("subject_content"));
        m.put("subjectStandard", rs.getString("subject_standard"));
        m.put("maintenStatus", rs.getString("mainten_status"));
        m.put("maintenResult", rs.getString("mainten_result"));
        return m;
    }

    public static Map<String, Object> mapRepair(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("repairId", rs.getLong("repair_id"));
        m.put("repairCode", rs.getString("repair_code"));
        m.put("repairName", rs.getString("repair_name"));
        m.put("machineryId", rs.getObject("machinery_id"));
        m.put("machineryCode", rs.getString("machinery_code"));
        m.put("machineryName", rs.getString("machinery_name"));
        m.put("machineryBrand", rs.getString("machinery_brand"));
        m.put("machinerySpec", rs.getString("machinery_spec"));
        m.put("machineryTypeId", rs.getObject("machinery_type_id"));
        m.put("requireDate", formatTs(rs.getTimestamp("require_date")));
        m.put("finishDate", formatTs(rs.getTimestamp("finish_date")));
        m.put("confirmDate", formatTs(rs.getTimestamp("confirm_date")));
        m.put("repairResult", rs.getString("repair_result"));
        m.put("acceptedId", rs.getObject("accepted_id"));
        m.put("acceptedName", rs.getString("accepted_name"));
        m.put("acceptedBy", rs.getString("accepted_by"));
        m.put("confirmId", rs.getObject("confirm_id"));
        m.put("confirmName", rs.getString("confirm_name"));
        m.put("confirmBy", rs.getString("confirm_by"));
        m.put("sourceDocType", rs.getString("source_doc_type"));
        m.put("sourceDocId", rs.getObject("source_doc_id"));
        m.put("sourceDocCode", rs.getString("source_doc_code"));
        m.put("status", rs.getString("status"));
        return m;
    }

    public static Map<String, Object> mapRepairLine(ResultSet rs) throws SQLException {
        Map<String, Object> m = baseAudit(rs);
        m.put("lineId", rs.getLong("line_id"));
        m.put("repairId", rs.getObject("repair_id"));
        m.put("subjectId", rs.getObject("subject_id"));
        m.put("subjectCode", rs.getString("subject_code"));
        m.put("subjectName", rs.getString("subject_name"));
        m.put("subjectType", rs.getString("subject_type"));
        m.put("subjectContent", rs.getString("subject_content"));
        m.put("subjectStandard", rs.getString("subject_standard"));
        m.put("malfunction", rs.getString("malfunction"));
        m.put("malfunctionUrl", rs.getString("malfunction_url"));
        m.put("repairDes", rs.getString("repair_des"));
        return m;
    }

    private static Map<String, Object> baseAudit(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
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

    static String formatTs(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        LocalDateTime ldt = ts.toLocalDateTime();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    }
}
