package com.yunshu.mes.masterdata.compat.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MdCompatRepository {

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public MdCompatRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<Map<String, Object>> listMdItems(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (tableExists(jdbc, "md_item")) {
            return listMdItemsFromTable(jdbc, query);
        }
        return listMdItemsLegacy(jdbc, query);
    }

    public long countMdItems(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (tableExists(jdbc, "md_item")) {
            return countMdItemsFromTable(jdbc, query);
        }
        return countMdItemsLegacy(jdbc, query);
    }

    private List<Map<String, Object>> listMdItemsFromTable(JdbcTemplate jdbc, Map<String, String> query) {
        int pageNum = intParam(query, "pageNum", 1);
        int pageSize = intParam(query, "pageSize", 10);
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        String itemCode = like(query.get("itemCode"));
        String itemName = like(query.get("itemName"));
        String itemTypeId = query.getOrDefault("itemTypeId", "0");
        String enableFlag = query.get("enableFlag");
        StringBuilder sql = new StringBuilder("""
                SELECT item_id AS itemId, item_code AS itemCode, item_name AS itemName,
                       IFNULL(specification,'') AS specification, IFNULL(unit_name,'') AS unitName,
                       item_or_product AS itemOrProduct, IFNULL(item_type_name,'') AS itemTypeName,
                       enable_flag AS enableFlag, create_time AS createTime
                FROM md_item WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (itemCode != null) { sql.append(" AND item_code LIKE ?"); args.add(itemCode); }
        if (itemName != null) { sql.append(" AND item_name LIKE ?"); args.add(itemName); }
        if ("1".equals(itemTypeId)) sql.append(" AND item_or_product = 'PRODUCT'");
        else if ("2".equals(itemTypeId)) sql.append(" AND item_or_product = 'ITEM'");
        if ("Y".equalsIgnoreCase(enableFlag)) sql.append(" AND enable_flag = 'Y'");
        sql.append(" ORDER BY item_id LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", rs.getLong("itemId"));
            row.put("itemCode", rs.getString("itemCode"));
            row.put("itemName", rs.getString("itemName"));
            row.put("specification", rs.getString("specification"));
            row.put("unitName", rs.getString("unitName"));
            row.put("itemOrProduct", rs.getString("itemOrProduct"));
            row.put("itemTypeName", rs.getString("itemTypeName"));
            row.put("enableFlag", rs.getString("enableFlag"));
            row.put("createTime", rs.getTimestamp("createTime"));
            return row;
        }, args.toArray());
    }

    private long countMdItemsFromTable(JdbcTemplate jdbc, Map<String, String> query) {
        String itemCode = like(query.get("itemCode"));
        String itemName = like(query.get("itemName"));
        String itemTypeId = query.getOrDefault("itemTypeId", "0");
        String enableFlag = query.get("enableFlag");
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM md_item WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (itemCode != null) { sql.append(" AND item_code LIKE ?"); args.add(itemCode); }
        if (itemName != null) { sql.append(" AND item_name LIKE ?"); args.add(itemName); }
        if ("1".equals(itemTypeId)) sql.append(" AND item_or_product = 'PRODUCT'");
        else if ("2".equals(itemTypeId)) sql.append(" AND item_or_product = 'ITEM'");
        if ("Y".equalsIgnoreCase(enableFlag)) sql.append(" AND enable_flag = 'Y'");
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    private List<Map<String, Object>> listMdItemsLegacy(JdbcTemplate jdbc, Map<String, String> query) {
        int pageNum = intParam(query, "pageNum", 1);
        int pageSize = intParam(query, "pageSize", 10);
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        String itemCode = like(query.get("itemCode"));
        String itemName = like(query.get("itemName"));
        String itemTypeId = query.getOrDefault("itemTypeId", "0");
        String enableFlag = query.get("enableFlag");

        StringBuilder sql = new StringBuilder("""
                SELECT * FROM (
                  SELECT p.product_id AS itemId, p.product_code AS itemCode, p.product_name AS itemName,
                         IFNULL(p.product_model, '') AS specification, '' AS unitName,
                         'PRODUCT' AS itemOrProduct, IFNULL(p.product_category, '产品') AS itemTypeName,
                         p.status AS status, p.created_at AS createTime
                  FROM product p WHERE p.is_deleted = 0
                  UNION ALL
                  SELECT m.material_id, m.material_code, m.material_name, '', IFNULL(u.unit_name, ''),
                         'ITEM', IFNULL(m.material_type, '物料'), m.status, m.created_at
                  FROM material m
                  LEFT JOIN uom u ON m.unit_id = u.unit_id
                  WHERE m.is_deleted = 0
                ) items WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (itemCode != null) {
            sql.append(" AND itemCode LIKE ?");
            args.add(itemCode);
        }
        if (itemName != null) {
            sql.append(" AND itemName LIKE ?");
            args.add(itemName);
        }
        if ("1".equals(itemTypeId)) {
            sql.append(" AND itemOrProduct = 'PRODUCT'");
        } else if ("2".equals(itemTypeId)) {
            sql.append(" AND itemOrProduct = 'ITEM'");
        }
        if ("Y".equalsIgnoreCase(enableFlag)) {
            sql.append(" AND status = 'ENABLED'");
        }
        sql.append(" ORDER BY itemId LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", rs.getLong("itemId"));
            row.put("itemCode", rs.getString("itemCode"));
            row.put("itemName", rs.getString("itemName"));
            row.put("specification", rs.getString("specification"));
            row.put("unitName", rs.getString("unitName"));
            row.put("itemOrProduct", rs.getString("itemOrProduct"));
            row.put("itemTypeName", rs.getString("itemTypeName"));
            row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
            row.put("createTime", rs.getTimestamp("createTime"));
            return row;
        }, args.toArray());
    }

    private long countMdItemsLegacy(JdbcTemplate jdbc, Map<String, String> query) {
        String itemCode = like(query.get("itemCode"));
        String itemName = like(query.get("itemName"));
        String itemTypeId = query.getOrDefault("itemTypeId", "0");
        String enableFlag = query.get("enableFlag");

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM (
                  SELECT p.product_id AS itemId, p.product_code AS itemCode, p.product_name AS itemName,
                         p.status AS status, 'PRODUCT' AS itemOrProduct
                  FROM product p WHERE p.is_deleted = 0
                  UNION ALL
                  SELECT m.material_id, m.material_code, m.material_name, m.status, 'ITEM'
                  FROM material m WHERE m.is_deleted = 0
                ) items WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (itemCode != null) {
            sql.append(" AND itemCode LIKE ?");
            args.add(itemCode);
        }
        if (itemName != null) {
            sql.append(" AND itemName LIKE ?");
            args.add(itemName);
        }
        if ("1".equals(itemTypeId)) {
            sql.append(" AND itemOrProduct = 'PRODUCT'");
        } else if ("2".equals(itemTypeId)) {
            sql.append(" AND itemOrProduct = 'ITEM'");
        }
        if ("Y".equalsIgnoreCase(enableFlag)) {
            sql.append(" AND status = 'ENABLED'");
        }
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    public Optional<Map<String, Object>> findMdItem(Long itemId, String itemOrProduct) {
        JdbcTemplate jdbc = requireJdbc();
        if ("PRODUCT".equalsIgnoreCase(itemOrProduct)) {
            return findProduct(jdbc, itemId);
        }
        if ("ITEM".equalsIgnoreCase(itemOrProduct)) {
            return findMaterial(jdbc, itemId);
        }
        return findProduct(jdbc, itemId).or(() -> findMaterial(jdbc, itemId));
    }

    public List<Map<String, Object>> listClients(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (tableExists(jdbc, "md_client")) {
            return listClientsFromTable(jdbc, query);
        }
        try {
            return jdbc.query("""
                    SELECT DISTINCT co.order_id AS clientId, co.customer_name AS clientName,
                           co.customer_name AS clientCode, co.customer_name AS clientNick,
                           'ENTERPRISE' AS clientType, 'Y' AS enableFlag
                    FROM customer_order co
                    WHERE co.customer_name IS NOT NULL AND co.customer_name <> ''
                    ORDER BY co.customer_name
                    """, (rs, n) -> mapClient(rs));
        } catch (Exception e) {
            return defaultClients();
        }
    }

    public long countClients(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_client")) {
            return listClients(query).size();
        }
        String code = like(query.get("clientCode"));
        String name = like(query.get("clientName"));
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM md_client WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (code != null) { sql.append(" AND client_code LIKE ?"); args.add(code); }
        if (name != null) { sql.append(" AND client_name LIKE ?"); args.add(name); }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findClient(Long clientId) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_client")) {
            return listClients(Map.of()).stream()
                    .filter(r -> clientId.equals(((Number) r.get("clientId")).longValue()))
                    .findFirst();
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT client_id, client_code, client_name, client_nick, client_type, client_des,
                       address, website, email, tel, contact1, contact1_tel, contact1_email,
                       contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark
                FROM md_client WHERE client_id = ?
                """, (rs, n) -> mapClientRow(rs), clientId);
        return rows.stream().findFirst();
    }

    public Long insertClient(Map<String, Object> body) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                INSERT INTO md_client (client_code, client_name, client_nick, client_type, client_des,
                  address, website, email, tel, contact1, contact1_tel, contact1_email,
                  contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark, create_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(3))
                """,
                str(body, "clientCode"), str(body, "clientName"), str(body, "clientNick"),
                str(body, "clientType"), str(body, "clientDes"), str(body, "address"),
                str(body, "website"), str(body, "email"), str(body, "tel"),
                str(body, "contact1"), str(body, "contact1Tel"), str(body, "contact1Email"),
                str(body, "contact2"), str(body, "contact2Tel"), str(body, "contact2Email"),
                str(body, "creditCode"), str(body, "enableFlag", "Y"), str(body, "remark", ""));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updateClient(Map<String, Object> body) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.update("""
                UPDATE md_client SET client_code=?, client_name=?, client_nick=?, client_type=?, client_des=?,
                  address=?, website=?, email=?, tel=?, contact1=?, contact1_tel=?, contact1_email=?,
                  contact2=?, contact2_tel=?, contact2_email=?, credit_code=?, enable_flag=?, remark=?,
                  update_time=NOW(3)
                WHERE client_id=?
                """,
                str(body, "clientCode"), str(body, "clientName"), str(body, "clientNick"),
                str(body, "clientType"), str(body, "clientDes"), str(body, "address"),
                str(body, "website"), str(body, "email"), str(body, "tel"),
                str(body, "contact1"), str(body, "contact1Tel"), str(body, "contact1Email"),
                str(body, "contact2"), str(body, "contact2Tel"), str(body, "contact2Email"),
                str(body, "creditCode"), str(body, "enableFlag", "Y"), str(body, "remark", ""),
                longVal(body, "clientId"));
    }

    public int deleteClient(Long clientId) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.update("DELETE FROM md_client WHERE client_id = ?", clientId);
    }

    public boolean clientCodeExists(String code, Long excludeId) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_client") || code == null || code.isBlank()) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM md_client WHERE client_code = ?", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM md_client WHERE client_code = ? AND client_id <> ?", Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public List<Map<String, Object>> listVendors(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (tableExists(jdbc, "md_vendor")) {
            return listVendorsFromTable(jdbc, query);
        }
        return defaultVendors();
    }

    public long countVendors(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_vendor")) {
            return listVendors(query).size();
        }
        String code = like(query.get("vendorCode"));
        String name = like(query.get("vendorName"));
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM md_vendor WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (code != null) { sql.append(" AND vendor_code LIKE ?"); args.add(code); }
        if (name != null) { sql.append(" AND vendor_name LIKE ?"); args.add(name); }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findVendor(Long vendorId) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_vendor")) {
            return listVendors(Map.of()).stream()
                    .filter(r -> vendorId.equals(((Number) r.get("vendorId")).longValue()))
                    .findFirst();
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT vendor_id, vendor_code, vendor_name, vendor_nick, vendor_type, vendor_des,
                       address, website, email, tel, contact1, contact1_tel, contact1_email,
                       contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark
                FROM md_vendor WHERE vendor_id = ?
                """, (rs, n) -> mapVendorRow(rs), vendorId);
        return rows.stream().findFirst();
    }

    public Long insertVendor(Map<String, Object> body) {
        JdbcTemplate jdbc = requireJdbc();
        jdbc.update("""
                INSERT INTO md_vendor (vendor_code, vendor_name, vendor_nick, vendor_type, vendor_des,
                  address, website, email, tel, contact1, contact1_tel, contact1_email,
                  contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark, create_time)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(3))
                """,
                str(body, "vendorCode"), str(body, "vendorName"), str(body, "vendorNick"),
                str(body, "vendorType"), str(body, "vendorDes"), str(body, "address"),
                str(body, "website"), str(body, "email"), str(body, "tel"),
                str(body, "contact1"), str(body, "contact1Tel"), str(body, "contact1Email"),
                str(body, "contact2"), str(body, "contact2Tel"), str(body, "contact2Email"),
                str(body, "creditCode"), str(body, "enableFlag", "Y"), str(body, "remark", ""));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updateVendor(Map<String, Object> body) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.update("""
                UPDATE md_vendor SET vendor_code=?, vendor_name=?, vendor_nick=?, vendor_type=?, vendor_des=?,
                  address=?, website=?, email=?, tel=?, contact1=?, contact1_tel=?, contact1_email=?,
                  contact2=?, contact2_tel=?, contact2_email=?, credit_code=?, enable_flag=?, remark=?,
                  update_time=NOW(3)
                WHERE vendor_id=?
                """,
                str(body, "vendorCode"), str(body, "vendorName"), str(body, "vendorNick"),
                str(body, "vendorType"), str(body, "vendorDes"), str(body, "address"),
                str(body, "website"), str(body, "email"), str(body, "tel"),
                str(body, "contact1"), str(body, "contact1Tel"), str(body, "contact1Email"),
                str(body, "contact2"), str(body, "contact2Tel"), str(body, "contact2Email"),
                str(body, "creditCode"), str(body, "enableFlag", "Y"), str(body, "remark", ""),
                longVal(body, "vendorId"));
    }

    public int deleteVendor(Long vendorId) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.update("DELETE FROM md_vendor WHERE vendor_id = ?", vendorId);
    }

    public boolean vendorCodeExists(String code, Long excludeId) {
        JdbcTemplate jdbc = requireJdbc();
        if (!tableExists(jdbc, "md_vendor") || code == null || code.isBlank()) {
            return false;
        }
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM md_vendor WHERE vendor_code = ?", Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM md_vendor WHERE vendor_code = ? AND vendor_id <> ?", Long.class, code, excludeId);
        return c != null && c > 0;
    }

    private List<Map<String, Object>> listClientsFromTable(JdbcTemplate jdbc, Map<String, String> query) {
        int pageNum = intParam(query, "pageNum", 1);
        int pageSize = intParam(query, "pageSize", 10);
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        String code = like(query.get("clientCode"));
        String name = like(query.get("clientName"));
        StringBuilder sql = new StringBuilder("""
                SELECT client_id, client_code, client_name, client_nick, client_type, client_des,
                       address, website, email, tel, contact1, contact1_tel, contact1_email,
                       contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark
                FROM md_client WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (code != null) { sql.append(" AND client_code LIKE ?"); args.add(code); }
        if (name != null) { sql.append(" AND client_name LIKE ?"); args.add(name); }
        sql.append(" ORDER BY client_id LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapClientRow(rs), args.toArray());
    }

    private List<Map<String, Object>> listVendorsFromTable(JdbcTemplate jdbc, Map<String, String> query) {
        int pageNum = intParam(query, "pageNum", 1);
        int pageSize = intParam(query, "pageSize", 10);
        int offset = Math.max(0, (pageNum - 1) * pageSize);
        String code = like(query.get("vendorCode"));
        String name = like(query.get("vendorName"));
        StringBuilder sql = new StringBuilder("""
                SELECT vendor_id, vendor_code, vendor_name, vendor_nick, vendor_type, vendor_des,
                       address, website, email, tel, contact1, contact1_tel, contact1_email,
                       contact2, contact2_tel, contact2_email, credit_code, enable_flag, remark
                FROM md_vendor WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (code != null) { sql.append(" AND vendor_code LIKE ?"); args.add(code); }
        if (name != null) { sql.append(" AND vendor_name LIKE ?"); args.add(name); }
        sql.append(" ORDER BY vendor_id LIMIT ? OFFSET ?");
        args.add(pageSize);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapVendorRow(rs), args.toArray());
    }

    public List<Map<String, Object>> listWorkstations(Map<String, String> query) {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query("""
                SELECT ws.station_id AS workstationId, ws.station_code AS workstationCode,
                       ws.station_name AS workstationName, pl.line_id AS lineId,
                       pl.line_name AS lineName, w.workshop_id AS workshopId,
                       w.workshop_code AS workshopCode, w.workshop_name AS workshopName,
                       ws.station_type AS processCode, ws.status AS status
                FROM workstation ws
                JOIN production_line pl ON ws.line_id = pl.line_id
                JOIN workshop w ON pl.workshop_id = w.workshop_id
                ORDER BY ws.station_id
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workstationId", rs.getLong("workstationId"));
            row.put("workstationCode", rs.getString("workstationCode"));
            row.put("workstationName", rs.getString("workstationName"));
            row.put("workshopId", rs.getLong("workshopId"));
            row.put("workshopCode", rs.getString("workshopCode"));
            row.put("workshopName", rs.getString("workshopName"));
            row.put("lineId", rs.getLong("lineId"));
            row.put("lineName", rs.getString("lineName"));
            row.put("processCode", rs.getString("processCode"));
            row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
            return row;
        });
    }

    public List<Map<String, Object>> listAllWorkshops() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query("""
                SELECT workshop_id AS workshopId, workshop_code AS workshopCode,
                       workshop_name AS workshopName, status
                FROM workshop ORDER BY workshop_id
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workshopId", rs.getLong("workshopId"));
            row.put("workshopCode", rs.getString("workshopCode"));
            row.put("workshopName", rs.getString("workshopName"));
            row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
            return row;
        });
    }

    public List<Map<String, Object>> listAllUnitmeasure() {
        JdbcTemplate jdbc = requireJdbc();
        return jdbc.query("""
                SELECT unit_id AS measureId, unit_code AS measureCode, unit_name AS measureName
                FROM uom ORDER BY unit_id
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("measureId", rs.getLong("measureId"));
            row.put("measureCode", rs.getString("measureCode"));
            row.put("measureName", rs.getString("measureName"));
            return row;
        });
    }

    public List<Map<String, Object>> listAllProcess() {
        JdbcTemplate jdbc = requireJdbc();
        if (tableExists(jdbc, "pro_process")) {
            return jdbc.query("""
                    SELECT process_id AS processId, process_code AS processCode, process_name AS processName,
                           enable_flag AS enableFlag
                    FROM pro_process ORDER BY process_id
                    """, (rs, n) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("processId", rs.getLong("processId"));
                row.put("processCode", rs.getString("processCode"));
                row.put("processName", rs.getString("processName"));
                row.put("enableFlag", rs.getString("enableFlag"));
                return row;
            });
        }
        return jdbc.query("""
                SELECT step_id AS processId, step_code AS processCode, step_name AS processName,
                       step_type AS processType, status
                FROM process_step ORDER BY step_id
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("processId", rs.getLong("processId"));
            row.put("processCode", rs.getString("processCode"));
            row.put("processName", rs.getString("processName"));
            row.put("processType", rs.getString("processType"));
            row.put("enableFlag", "ENABLED".equals(rs.getString("status")) ? "Y" : "N");
            return row;
        });
    }

    private Optional<Map<String, Object>> findProduct(JdbcTemplate jdbc, Long id) {
        List<Map<String, Object>> list = jdbc.query("""
                SELECT product_id AS itemId, product_code AS itemCode, product_name AS itemName,
                       IFNULL(product_model, '') AS specification, '' AS unitName, 'PRODUCT' AS itemOrProduct
                FROM product WHERE product_id = ? AND is_deleted = 0
                """, (rs, n) -> mapItem(rs), id);
        return list.stream().findFirst();
    }

    private Optional<Map<String, Object>> findMaterial(JdbcTemplate jdbc, Long id) {
        List<Map<String, Object>> list = jdbc.query("""
                SELECT m.material_id AS itemId, m.material_code AS itemCode, m.material_name AS itemName,
                       '' AS specification, IFNULL(u.unit_name, '') AS unitName, 'ITEM' AS itemOrProduct
                FROM material m LEFT JOIN uom u ON m.unit_id = u.unit_id
                WHERE m.material_id = ? AND m.is_deleted = 0
                """, (rs, n) -> mapItem(rs), id);
        return list.stream().findFirst();
    }

    private Map<String, Object> mapItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemId", rs.getLong("itemId"));
        row.put("itemCode", rs.getString("itemCode"));
        row.put("itemName", rs.getString("itemName"));
        row.put("specification", rs.getString("specification"));
        row.put("unitName", rs.getString("unitName"));
        row.put("itemOrProduct", rs.getString("itemOrProduct"));
        return row;
    }

    private Map<String, Object> mapClientRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("clientId", rs.getLong("client_id"));
        row.put("clientCode", rs.getString("client_code"));
        row.put("clientName", rs.getString("client_name"));
        row.put("clientNick", rs.getString("client_nick"));
        row.put("clientType", rs.getString("client_type"));
        row.put("clientDes", rs.getString("client_des"));
        row.put("address", rs.getString("address"));
        row.put("website", rs.getString("website"));
        row.put("email", rs.getString("email"));
        row.put("tel", rs.getString("tel"));
        row.put("contact1", rs.getString("contact1"));
        row.put("contact1Tel", rs.getString("contact1_tel"));
        row.put("contact1Email", rs.getString("contact1_email"));
        row.put("contact2", rs.getString("contact2"));
        row.put("contact2Tel", rs.getString("contact2_tel"));
        row.put("contact2Email", rs.getString("contact2_email"));
        row.put("creditCode", rs.getString("credit_code"));
        row.put("enableFlag", rs.getString("enable_flag"));
        row.put("remark", rs.getString("remark"));
        return row;
    }

    private Map<String, Object> mapVendorRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("vendorId", rs.getLong("vendor_id"));
        row.put("vendorCode", rs.getString("vendor_code"));
        row.put("vendorName", rs.getString("vendor_name"));
        row.put("vendorNick", rs.getString("vendor_nick"));
        row.put("vendorType", rs.getString("vendor_type"));
        row.put("vendorDes", rs.getString("vendor_des"));
        row.put("address", rs.getString("address"));
        row.put("website", rs.getString("website"));
        row.put("email", rs.getString("email"));
        row.put("tel", rs.getString("tel"));
        row.put("contact1", rs.getString("contact1"));
        row.put("contact1Tel", rs.getString("contact1_tel"));
        row.put("contact1Email", rs.getString("contact1_email"));
        row.put("contact2", rs.getString("contact2"));
        row.put("contact2Tel", rs.getString("contact2_tel"));
        row.put("contact2Email", rs.getString("contact2_email"));
        row.put("creditCode", rs.getString("credit_code"));
        row.put("enableFlag", rs.getString("enable_flag"));
        row.put("remark", rs.getString("remark"));
        return row;
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

    private Map<String, Object> mapClient(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("clientId", rs.getLong("clientId"));
        row.put("clientCode", rs.getString("clientCode"));
        row.put("clientName", rs.getString("clientName"));
        row.put("clientNick", rs.getString("clientNick"));
        row.put("clientType", rs.getString("clientType"));
        row.put("enableFlag", rs.getString("enableFlag"));
        return row;
    }

    private List<Map<String, Object>> defaultClients() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(client(1L, "C001", "华东电机有限公司"));
        rows.add(client(2L, "C002", "深圳智造科技"));
        return rows;
    }

    private List<Map<String, Object>> defaultVendors() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(vendor(1L, "V001", "苏州精密外协"));
        rows.add(vendor(2L, "V002", "东莞采购供应商"));
        return rows;
    }

    private Map<String, Object> client(Long id, String code, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientId", id);
        m.put("clientCode", code);
        m.put("clientName", name);
        m.put("clientNick", name);
        m.put("clientType", "ENTERPRISE");
        m.put("enableFlag", "Y");
        return m;
    }

    private Map<String, Object> vendor(Long id, String code, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vendorId", id);
        m.put("vendorCode", code);
        m.put("vendorName", name);
        m.put("vendorNick", name);
        m.put("enableFlag", "Y");
        return m;
    }

    private Map<String, String> withPage(Map<String, String> query, int pageNum, int pageSize) {
        Map<String, String> q = new LinkedHashMap<>(query);
        q.put("pageNum", String.valueOf(pageNum));
        q.put("pageSize", String.valueOf(pageSize));
        return q;
    }

    private int intParam(Map<String, String> query, String key, int defaultVal) {
        try {
            return Integer.parseInt(query.getOrDefault(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String like(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        return "%" + val.trim() + "%";
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        try {
            Long c = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                    Long.class, table);
            return c != null && c > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private JdbcTemplate requireJdbc() {
        JdbcTemplate j = jdbcTemplateProvider.getIfAvailable();
        if (j == null) {
            throw new DataAccessResourceFailureException("no jdbc");
        }
        return j;
    }
}
