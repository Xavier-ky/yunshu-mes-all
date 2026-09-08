package com.yunshu.mes.print.compat.repository;

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
public class PrintCompatRepository {

    private final JdbcTemplate jdbc;

    public PrintCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> searchTemplates(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT template_id, template_code, template_name, template_type, template_json, paper_type,
                       template_width, template_height, is_default, enable_flag, template_pic, remark, create_time
                FROM print_template WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("templateName"))) {
            sql.append(" AND template_name LIKE ?");
            args.add("%" + params.get("templateName").trim() + "%");
        }
        if (StringUtils.hasText(params.get("templateType"))) {
            sql.append(" AND template_type = ?");
            args.add(params.get("templateType").trim());
        }
        sql.append(" ORDER BY template_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::printTemplateRow, args.toArray());
    }

    public long countTemplates(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM print_template WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("templateName"))) {
            sql.append(" AND template_name LIKE ?");
            args.add("%" + params.get("templateName").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findTemplateById(Long id) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT template_id, template_code, template_name, template_type, template_json, paper_type,
                       template_width, template_height, is_default, enable_flag, template_pic, remark, create_time
                FROM print_template WHERE template_id = ?
                """, AnalyticsJdbcHelper::printTemplateRow, id);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findTemplateByType(String templateType) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT template_id, template_code, template_name, template_type, template_json, paper_type,
                       template_width, template_height, is_default, enable_flag, template_pic, remark, create_time
                FROM print_template WHERE template_type = ? AND enable_flag = 'Y' LIMIT 1
                """, AnalyticsJdbcHelper::printTemplateRow, templateType);
        return rows.stream().findFirst();
    }

    public Long insertTemplate(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO print_template (template_code, template_name, template_type, template_json,
                      paper_type, template_width, template_height, is_default, enable_flag, template_pic, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("templateCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("templateName")));
            ps.setString(i++, SysCompatHelper.str(body.get("templateType")));
            ps.setString(i++, SysCompatHelper.str(body.get("templateJson")));
            ps.setString(i++, SysCompatHelper.str(body.get("paperType")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("templateWidth")));
            ps.setObject(i++, SysCompatHelper.intObj(body.get("templateHeight")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("isDefault"), "N"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("enableFlag"), "Y"));
            ps.setString(i++, SysCompatHelper.str(body.get("templatePic")));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateTemplate(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE print_template SET template_code=?, template_name=?, template_type=?, template_json=?,
                  paper_type=?, template_width=?, template_height=?, is_default=?, enable_flag=?, template_pic=?, remark=?,
                  update_by=?, update_time=?
                WHERE template_id=?
                """,
                SysCompatHelper.str(body.get("templateCode")),
                SysCompatHelper.str(body.get("templateName")),
                SysCompatHelper.str(body.get("templateType")),
                SysCompatHelper.str(body.get("templateJson")),
                SysCompatHelper.str(body.get("paperType")),
                SysCompatHelper.intObj(body.get("templateWidth")),
                SysCompatHelper.intObj(body.get("templateHeight")),
                SysCompatHelper.strOr(body.get("isDefault"), "N"),
                SysCompatHelper.strOr(body.get("enableFlag"), "Y"),
                SysCompatHelper.str(body.get("templatePic")),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                now,
                SysCompatHelper.longVal(body.get("templateId")));
    }

    public int deleteTemplates(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM print_template WHERE template_id IN (" + placeholders + ")", ids.toArray());
    }

    public List<Map<String, Object>> searchClients(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT client_id, client_code, client_name, client_ip, client_port, client_token, status,
                       workshop_id, workshop_code, workshop_name, workstation_id, workstation_code, workstation_name, create_time
                FROM print_client WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("clientName"))) {
            sql.append(" AND client_name LIKE ?");
            args.add("%" + params.get("clientName").trim() + "%");
        }
        sql.append(" ORDER BY client_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::printClientRow, args.toArray());
    }

    public long countClients(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM print_client WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("clientName"))) {
            sql.append(" AND client_name LIKE ?");
            args.add("%" + params.get("clientName").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Long insertClient(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO print_client (client_code, client_name, client_ip, client_port, client_token, status,
                      workshop_id, workshop_code, workshop_name, workstation_id, workstation_code, workstation_name,
                      create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("clientCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("clientName")));
            ps.setString(i++, SysCompatHelper.str(body.get("clientIp")));
            ps.setObject(i++, SysCompatHelper.longObj(body.get("clientPort")));
            ps.setString(i++, SysCompatHelper.str(body.get("clientToken")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "OFFLINE"));
            ps.setObject(i++, SysCompatHelper.longObj(body.get("workshopId")));
            ps.setString(i++, SysCompatHelper.str(body.get("workshopCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("workshopName")));
            ps.setObject(i++, SysCompatHelper.longObj(body.get("workstationId")));
            ps.setString(i++, SysCompatHelper.str(body.get("workstationCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("workstationName")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateClient(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE print_client SET client_code=?, client_name=?, client_ip=?, client_port=?, client_token=?, status=?,
                  workshop_id=?, workshop_code=?, workshop_name=?, workstation_id=?, workstation_code=?, workstation_name=?,
                  update_by=?, update_time=?
                WHERE client_id=?
                """,
                SysCompatHelper.str(body.get("clientCode")),
                SysCompatHelper.str(body.get("clientName")),
                SysCompatHelper.str(body.get("clientIp")),
                SysCompatHelper.longObj(body.get("clientPort")),
                SysCompatHelper.str(body.get("clientToken")),
                SysCompatHelper.str(body.get("status")),
                SysCompatHelper.longObj(body.get("workshopId")),
                SysCompatHelper.str(body.get("workshopCode")),
                SysCompatHelper.str(body.get("workshopName")),
                SysCompatHelper.longObj(body.get("workstationId")),
                SysCompatHelper.str(body.get("workstationCode")),
                SysCompatHelper.str(body.get("workstationName")),
                SysCompatHelper.currentUsername(),
                now,
                SysCompatHelper.longVal(body.get("clientId")));
    }

    public int deleteClients(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM print_client WHERE client_id IN (" + placeholders + ")", ids.toArray());
    }

    public List<Map<String, Object>> listWorkstationsForClient() {
        return jdbc.query("""
                SELECT w.station_id AS workstation_id, w.station_code AS workstation_code, w.station_name AS workstation_name,
                       ws.workshop_id, ws.workshop_code, ws.workshop_name
                FROM workstation w
                JOIN production_line pl ON w.line_id = pl.line_id
                JOIN workshop ws ON pl.workshop_id = ws.workshop_id
                WHERE w.status = 'ENABLED'
                ORDER BY ws.workshop_id, w.station_id
                """, AnalyticsJdbcHelper::workstationDictRow);
    }

    public List<Map<String, Object>> searchPrinters(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT printer_id, client_id, printer_code, printer_type, printer_name, brand, printer_model,
                       connection_type, printer_url, printer_ip, printer_port, enable_flag, status, default_flag, remark, create_time
                FROM print_printer_config WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("clientId"))) {
            sql.append(" AND client_id = ?");
            args.add(Long.parseLong(params.get("clientId").trim()));
        }
        if (StringUtils.hasText(params.get("printerName"))) {
            sql.append(" AND printer_name LIKE ?");
            args.add("%" + params.get("printerName").trim() + "%");
        }
        sql.append(" ORDER BY printer_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::printPrinterRow, args.toArray());
    }

    public long countPrinters(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM print_printer_config WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("clientId"))) {
            sql.append(" AND client_id = ?");
            args.add(Long.parseLong(params.get("clientId").trim()));
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findPrinterById(Long id) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT printer_id, client_id, printer_code, printer_type, printer_name, brand, printer_model,
                       connection_type, printer_url, printer_ip, printer_port, enable_flag, status, default_flag, remark, create_time
                FROM print_printer_config WHERE printer_id = ?
                """, AnalyticsJdbcHelper::printPrinterRow, id);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findDefaultPrinterByClientId(Long clientId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT printer_id, client_id, printer_code, printer_type, printer_name, brand, printer_model,
                       connection_type, printer_url, printer_ip, printer_port, enable_flag, status, default_flag, remark, create_time
                FROM print_printer_config WHERE client_id = ? AND default_flag = 'Y' LIMIT 1
                """, AnalyticsJdbcHelper::printPrinterRow, clientId);
        return rows.stream().findFirst();
    }

    public Long insertPrinter(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO print_printer_config (client_id, printer_code, printer_type, printer_name, brand, printer_model,
                      connection_type, printer_url, printer_ip, printer_port, enable_flag, status, default_flag, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setObject(i++, SysCompatHelper.longObj(body.get("clientId")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerType")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerName")));
            ps.setString(i++, SysCompatHelper.str(body.get("brand")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerModel")));
            ps.setString(i++, SysCompatHelper.str(body.get("connectionType")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerUrl")));
            ps.setString(i++, SysCompatHelper.str(body.get("printerIp")));
            ps.setObject(i++, SysCompatHelper.longObj(body.get("printerPort")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("enableFlag"), "Y"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "OFFLINE"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("defaultFlag"), "N"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updatePrinter(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE print_printer_config SET client_id=?, printer_code=?, printer_type=?, printer_name=?, brand=?, printer_model=?,
                  connection_type=?, printer_url=?, printer_ip=?, printer_port=?, enable_flag=?, status=?, default_flag=?, remark=?,
                  update_by=?, update_time=?
                WHERE printer_id=?
                """,
                SysCompatHelper.longObj(body.get("clientId")),
                SysCompatHelper.str(body.get("printerCode")),
                SysCompatHelper.str(body.get("printerType")),
                SysCompatHelper.str(body.get("printerName")),
                SysCompatHelper.str(body.get("brand")),
                SysCompatHelper.str(body.get("printerModel")),
                SysCompatHelper.str(body.get("connectionType")),
                SysCompatHelper.str(body.get("printerUrl")),
                SysCompatHelper.str(body.get("printerIp")),
                SysCompatHelper.longObj(body.get("printerPort")),
                SysCompatHelper.strOr(body.get("enableFlag"), "Y"),
                SysCompatHelper.str(body.get("status")),
                SysCompatHelper.strOr(body.get("defaultFlag"), "N"),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                now,
                SysCompatHelper.longVal(body.get("printerId")));
    }

    public int deletePrinters(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM print_printer_config WHERE printer_id IN (" + placeholders + ")", ids.toArray());
    }

    public int clearDefaultPrinterForClient(Long clientId) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE print_printer_config SET default_flag = 'N', update_by = ?, update_time = ?
                WHERE client_id = ?
                """, SysCompatHelper.currentUsername(), now, clientId);
    }

    public int setDefaultPrinter(Long printerId, Long clientId) {
        clearDefaultPrinterForClient(clientId);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE print_printer_config SET default_flag = 'Y', update_by = ?, update_time = ?
                WHERE printer_id = ? AND client_id = ?
                """, SysCompatHelper.currentUsername(), now, printerId, clientId);
    }
}
