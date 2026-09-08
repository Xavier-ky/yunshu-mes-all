package com.yunshu.mes.reporting.compat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnalyticsJdbcHelper {

    private AnalyticsJdbcHelper() {}

    public static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static Long longObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    public static Double doubleObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }

    public static Timestamp ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return rs.wasNull() ? null : t;
    }

    public static Map<String, Object> ureportRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id_"));
        m.put("name", rs.getString("name_"));
        m.put("createTime", ts(rs, "create_time_"));
        m.put("updateTime", ts(rs, "update_time_"));
        return m;
    }

    public static Map<String, Object> ureportDetailRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = ureportRow(rs, n);
        m.put("content", rs.getString("content_"));
        return m;
    }

    public static Map<String, Object> chartRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("chartId", rs.getLong("chart_id"));
        m.put("chartCode", rs.getString("chart_code"));
        m.put("chartName", rs.getString("chart_name"));
        m.put("chartType", rs.getString("chart_type"));
        m.put("businessType", rs.getString("business_type"));
        m.put("api", rs.getString("api"));
        m.put("options", rs.getString("options"));
        m.put("chartPic", rs.getString("chart_pic"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("remark", rs.getString("remark"));
        m.put("createBy", rs.getString("create_by"));
        m.put("createTime", ts(rs, "create_time"));
        m.put("updateBy", rs.getString("update_by"));
        m.put("updateTime", ts(rs, "update_time"));
        return m;
    }

    public static Map<String, Object> procardRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cardId", rs.getLong("card_id"));
        m.put("cardCode", rs.getString("card_code"));
        m.put("workorderId", rs.getObject("workorder_id"));
        m.put("workorderCode", rs.getString("workorder_code"));
        m.put("workorderName", rs.getString("workorder_name"));
        m.put("batchCode", rs.getString("batch_code"));
        m.put("itemCode", rs.getString("item_code"));
        m.put("itemName", rs.getString("item_name"));
        m.put("specification", rs.getString("specification"));
        m.put("unitOfMeasure", rs.getString("unit_of_measure"));
        m.put("quantityTransfered", rs.getObject("quantity_transfered"));
        m.put("status", rs.getString("status"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", ts(rs, "create_time"));
        return m;
    }

    public static Map<String, Object> cardProcessRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordId", rs.getLong("record_id"));
        m.put("cardId", rs.getLong("card_id"));
        m.put("cardCode", rs.getString("card_code"));
        m.put("seqNum", rs.getInt("seq_num"));
        m.put("processCode", rs.getString("process_code"));
        m.put("processName", rs.getString("process_name"));
        m.put("inputTime", ts(rs, "input_time"));
        m.put("outputTime", ts(rs, "output_time"));
        m.put("quantityInput", rs.getObject("quantity_input"));
        m.put("quantityOutput", rs.getObject("quantity_output"));
        m.put("workstationName", rs.getString("workstation_name"));
        m.put("userName", rs.getString("user_name"));
        return m;
    }

    public static Map<String, Object> snProcessRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordId", rs.getLong("record_id"));
        m.put("snId", rs.getLong("sn_id"));
        m.put("snCode", rs.getString("sn_code"));
        m.put("seqNum", rs.getInt("seq_num"));
        m.put("processCode", rs.getString("process_code"));
        m.put("processName", rs.getString("process_name"));
        m.put("inputTime", ts(rs, "input_time"));
        m.put("outputTime", ts(rs, "output_time"));
        m.put("workstationName", rs.getString("workstation_name"));
        return m;
    }

    public static Map<String, Object> printTemplateRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("templateId", rs.getLong("template_id"));
        m.put("templateCode", rs.getString("template_code"));
        m.put("templateName", rs.getString("template_name"));
        m.put("templateType", rs.getString("template_type"));
        m.put("templateJson", rs.getString("template_json"));
        m.put("paperType", rs.getString("paper_type"));
        m.put("templateWidth", rs.getObject("template_width"));
        m.put("templateHeight", rs.getObject("template_height"));
        m.put("isDefault", rs.getString("is_default"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("templatePic", rs.getString("template_pic"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", ts(rs, "create_time"));
        return m;
    }

    public static Map<String, Object> printClientRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientId", rs.getLong("client_id"));
        m.put("clientCode", rs.getString("client_code"));
        m.put("clientName", rs.getString("client_name"));
        m.put("clientIp", rs.getString("client_ip"));
        m.put("clientPort", rs.getObject("client_port"));
        m.put("clientToken", rs.getString("client_token"));
        m.put("status", rs.getString("status"));
        m.put("workshopId", rs.getObject("workshop_id"));
        m.put("workshopCode", rs.getString("workshop_code"));
        m.put("workshopName", rs.getString("workshop_name"));
        m.put("workstationId", rs.getObject("workstation_id"));
        m.put("workstationCode", rs.getString("workstation_code"));
        m.put("workstationName", rs.getString("workstation_name"));
        m.put("createTime", ts(rs, "create_time"));
        return m;
    }

    public static Map<String, Object> printPrinterRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("printerId", rs.getLong("printer_id"));
        m.put("clientId", rs.getObject("client_id"));
        m.put("printerCode", rs.getString("printer_code"));
        m.put("printerType", rs.getString("printer_type"));
        m.put("printerName", rs.getString("printer_name"));
        m.put("brand", rs.getString("brand"));
        m.put("printerModel", rs.getString("printer_model"));
        m.put("connectionType", rs.getString("connection_type"));
        m.put("printerUrl", rs.getString("printer_url"));
        m.put("printerIp", rs.getString("printer_ip"));
        m.put("printerPort", rs.getObject("printer_port"));
        m.put("enableFlag", rs.getString("enable_flag"));
        m.put("status", rs.getString("status"));
        m.put("defaultFlag", rs.getString("default_flag"));
        m.put("remark", rs.getString("remark"));
        m.put("createTime", ts(rs, "create_time"));
        return m;
    }

    public static Map<String, Object> workstationDictRow(ResultSet rs, int n) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("workstationId", rs.getLong("workstation_id"));
        m.put("workstationCode", rs.getString("workstation_code"));
        m.put("workstationName", rs.getString("workstation_name"));
        m.put("workshopId", rs.getLong("workshop_id"));
        m.put("workshopCode", rs.getString("workshop_code"));
        m.put("workshopName", rs.getString("workshop_name"));
        return m;
    }
}
