package com.yunshu.mes.inventory.compat.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class WmStockRepository {

    private static final String BASE = """
            SELECT s.* FROM wm_material_stock s WHERE 1=1
            """;

    private final JdbcTemplate jdbc;

    public WmStockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> query, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE);
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        sql.append(" ORDER BY s.material_stock_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> mapRow(rs), args.toArray());
    }

    public long count(Map<String, String> query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM wm_material_stock s WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, query);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> binMap(Map<String, String> query) {
        if (!StringUtils.hasText(query.get("warehouseId"))) {
            return Optional.empty();
        }
        long warehouseId = Long.parseLong(query.get("warehouseId").trim());
        List<Map<String, Object>> whRows = jdbc.query("""
                SELECT warehouse_id, warehouse_code, warehouse_name, warehouse_type
                FROM warehouse WHERE warehouse_id = ?
                """, (rs, n) -> {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("warehouseId", rs.getLong("warehouse_id"));
            w.put("warehouseCode", rs.getString("warehouse_code"));
            w.put("warehouseName", rs.getString("warehouse_name"));
            w.put("warehouseType", rs.getString("warehouse_type"));
            return w;
        }, warehouseId);
        if (whRows.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder zoneSql = new StringBuilder("""
                SELECT zone_id, zone_code, zone_name, warehouse_id
                FROM storage_zone WHERE warehouse_id = ?
                """);
        List<Object> zoneArgs = new ArrayList<>();
        zoneArgs.add(warehouseId);
        if (StringUtils.hasText(query.get("locationId"))) {
            zoneSql.append(" AND zone_id = ?");
            zoneArgs.add(Long.parseLong(query.get("locationId").trim()));
        }
        zoneSql.append(" ORDER BY zone_id");
        List<Map<String, Object>> zones = jdbc.query(zoneSql.toString(), (rs, n) -> {
            Map<String, Object> z = new LinkedHashMap<>();
            z.put("locationId", rs.getLong("zone_id"));
            z.put("locationCode", rs.getString("zone_code"));
            z.put("locationName", rs.getString("zone_name"));
            z.put("warehouseId", rs.getLong("warehouse_id"));
            return z;
        }, zoneArgs.toArray());

        if (zones.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("warehouse", whRows.get(0));
            empty.put("zones", List.of());
            empty.put("bins", List.of());
            empty.put("summary", emptySummary());
            return Optional.of(empty);
        }

        Set<Long> zoneIds = new LinkedHashSet<>();
        for (Map<String, Object> z : zones) {
            zoneIds.add((Long) z.get("locationId"));
        }
        String zoneIn = zoneIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("0");
        List<Object> binArgs = new ArrayList<>(zoneIds);
        List<Map<String, Object>> binRows = jdbc.query("""
                SELECT sb.bin_id, sb.bin_code, sb.bin_name, sb.zone_id,
                       sb.position_x, sb.position_y, sb.position_z,
                       sb.max_loa, sb.enable_flag, sb.frozen_flag AS bin_frozen_flag,
                       sz.zone_code, sz.zone_name
                FROM storage_bin sb
                JOIN storage_zone sz ON sz.zone_id = sb.zone_id
                WHERE sb.zone_id IN (""" + zoneIn + """
                ) AND IFNULL(sb.enable_flag, 'Y') = 'Y'
                ORDER BY sb.position_y, sb.position_x, sb.bin_id
                """, (rs, n) -> {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("areaId", rs.getLong("bin_id"));
            b.put("areaCode", rs.getString("bin_code"));
            b.put("areaName", rs.getString("bin_name"));
            b.put("locationId", rs.getLong("zone_id"));
            b.put("locationCode", rs.getString("zone_code"));
            b.put("locationName", rs.getString("zone_name"));
            b.put("positionX", rs.getObject("position_x"));
            b.put("positionY", rs.getObject("position_y"));
            b.put("positionZ", rs.getObject("position_z"));
            b.put("maxLoa", rs.getObject("max_loa"));
            b.put("enableFlag", rs.getString("enable_flag"));
            b.put("binFrozenFlag", rs.getString("bin_frozen_flag"));
            return b;
        }, binArgs.toArray());

        List<Object> stockArgs = new ArrayList<>();
        stockArgs.add(warehouseId);
        StringBuilder stockSql = new StringBuilder("""
                SELECT s.area_id, s.item_code, s.item_name, s.batch_code,
                       s.quantity_onhand, s.quantity_reserved, s.frozen_flag, s.expire_date
                FROM wm_material_stock s
                WHERE s.warehouse_id = ? AND s.area_id IS NOT NULL
                """);
        if (StringUtils.hasText(query.get("locationId"))) {
            stockSql.append(" AND s.location_id = ?");
            stockArgs.add(Long.parseLong(query.get("locationId").trim()));
        }
        stockSql.append(" ORDER BY s.area_id, s.material_stock_id");
        List<Map<String, Object>> stockRows = jdbc.query(stockSql.toString(), (rs, n) -> {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("areaId", rs.getLong("area_id"));
            s.put("itemCode", rs.getString("item_code"));
            s.put("itemName", rs.getString("item_name"));
            s.put("batchCode", rs.getString("batch_code"));
            s.put("quantityOnhand", rs.getObject("quantity_onhand"));
            s.put("quantityReserved", rs.getObject("quantity_reserved"));
            s.put("frozenFlag", rs.getString("frozen_flag"));
            s.put("expireDate", rs.getTimestamp("expire_date"));
            return s;
        }, stockArgs.toArray());

        Map<Long, List<Map<String, Object>>> stocksByBin = new LinkedHashMap<>();
        for (Map<String, Object> stock : stockRows) {
            Long areaId = (Long) stock.get("areaId");
            stocksByBin.computeIfAbsent(areaId, k -> new ArrayList<>()).add(stock);
        }

        double maxQtyInZone = 0;
        List<Map<String, Object>> bins = new ArrayList<>();
        for (Map<String, Object> bin : binRows) {
            Long areaId = (Long) bin.get("areaId");
            List<Map<String, Object>> stocks = stocksByBin.getOrDefault(areaId, List.of());
            double qtyOnhand = 0;
            double qtyReserved = 0;
            int skuCount = 0;
            boolean hasFrozen = "Y".equals(bin.get("binFrozenFlag"));
            boolean hasExpiring = false;
            Set<String> skuSet = new LinkedHashSet<>();
            for (Map<String, Object> st : stocks) {
                double q = doubleVal(st.get("quantityOnhand"), 0d);
                qtyOnhand += q;
                qtyReserved += doubleVal(st.get("quantityReserved"), 0d);
                if (st.get("itemCode") != null) {
                    skuSet.add(String.valueOf(st.get("itemCode")));
                }
                if ("Y".equals(st.get("frozenFlag"))) {
                    hasFrozen = true;
                }
                if (isExpiringSoon(st.get("expireDate"))) {
                    hasExpiring = true;
                }
            }
            skuCount = skuSet.size();
            maxQtyInZone = Math.max(maxQtyInZone, qtyOnhand);

            Map<String, Object> enriched = new LinkedHashMap<>(bin);
            enriched.put("skuCount", skuCount);
            enriched.put("quantityOnhand", qtyOnhand);
            enriched.put("quantityReserved", qtyReserved);
            enriched.put("hasFrozen", hasFrozen);
            enriched.put("hasExpiring", hasExpiring);
            enriched.put("stocks", stocks);
            bins.add(enriched);
        }

        for (Map<String, Object> bin : bins) {
            double qty = doubleVal(bin.get("quantityOnhand"), 0d);
            double maxLoa = doubleVal(bin.get("maxLoa"), 0d);
            double occupancyPct;
            if (maxLoa > 0) {
                occupancyPct = Math.min(100, Math.round(qty / maxLoa * 1000) / 10.0);
            } else if (maxQtyInZone > 0) {
                occupancyPct = Math.round(qty / maxQtyInZone * 1000) / 10.0;
            } else {
                occupancyPct = 0;
            }
            bin.put("occupancyPct", occupancyPct);
            bin.put("status", resolveBinStatus(bin));
        }

        int totalBins = bins.size();
        int occupiedBins = 0;
        int emptyBins = 0;
        int frozenBins = 0;
        int expiringBins = 0;
        for (Map<String, Object> bin : bins) {
            String status = String.valueOf(bin.get("status"));
            if ("empty".equals(status)) {
                emptyBins++;
            } else {
                occupiedBins++;
            }
            if (Boolean.TRUE.equals(bin.get("hasFrozen")) || "frozen".equals(status)) {
                frozenBins++;
            }
            if (Boolean.TRUE.equals(bin.get("hasExpiring")) || "expiring".equals(status)) {
                expiringBins++;
            }
        }
        double occupancyRate = totalBins > 0
                ? Math.round(occupiedBins * 1000.0 / totalBins) / 10.0
                : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBins", totalBins);
        summary.put("occupiedBins", occupiedBins);
        summary.put("emptyBins", emptyBins);
        summary.put("frozenBins", frozenBins);
        summary.put("expiringBins", expiringBins);
        summary.put("occupancyRate", occupancyRate);

        Map<Long, List<Map<String, Object>>> binsByZone = new LinkedHashMap<>();
        for (Map<String, Object> bin : bins) {
            Long zid = (Long) bin.get("locationId");
            binsByZone.computeIfAbsent(zid, k -> new ArrayList<>()).add(bin);
        }
        List<Map<String, Object>> zonesWithBins = new ArrayList<>();
        for (Map<String, Object> z : zones) {
            Map<String, Object> zw = new LinkedHashMap<>(z);
            zw.put("bins", binsByZone.getOrDefault((Long) z.get("locationId"), List.of()));
            zonesWithBins.add(zw);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("warehouse", whRows.get(0));
        result.put("zones", zonesWithBins);
        result.put("bins", bins);
        result.put("summary", summary);
        return Optional.of(result);
    }

    private static Map<String, Object> emptySummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBins", 0);
        summary.put("occupiedBins", 0);
        summary.put("emptyBins", 0);
        summary.put("frozenBins", 0);
        summary.put("expiringBins", 0);
        summary.put("occupancyRate", 0);
        return summary;
    }

    private static String resolveBinStatus(Map<String, Object> bin) {
        if (!"Y".equals(bin.get("enableFlag")) && bin.get("enableFlag") != null) {
            return "disabled";
        }
        int skuCount = ((Number) bin.getOrDefault("skuCount", 0)).intValue();
        double qty = doubleVal(bin.get("quantityOnhand"), 0d);
        if (skuCount == 0 || qty <= 0) {
            return "empty";
        }
        if ("Y".equals(bin.get("binFrozenFlag")) || Boolean.TRUE.equals(bin.get("hasFrozen"))) {
            return "frozen";
        }
        if (Boolean.TRUE.equals(bin.get("hasExpiring"))) {
            return "expiring";
        }
        double maxLoa = doubleVal(bin.get("maxLoa"), 0d);
        if (maxLoa > 0 && qty / maxLoa < 0.15) {
            return "low";
        }
        if (skuCount == 1 && qty < 100) {
            return "low";
        }
        return "occupied";
    }

    private boolean isExpiringSoon(Object expireDate) {
        if (expireDate == null) {
            return false;
        }
        try {
            java.sql.Timestamp ts = (java.sql.Timestamp) expireDate;
            java.time.LocalDate exp = ts.toLocalDateTime().toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            return !exp.isBefore(today) && !exp.isAfter(today.plusDays(30));
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", jdbc.queryForMap("""
                SELECT COUNT(DISTINCT item_code) AS skuCount,
                       COALESCE(SUM(quantity_onhand), 0) AS quantityOnhand,
                       COALESCE(SUM(CASE WHEN frozen_flag = 'Y' THEN 1 ELSE 0 END), 0) AS frozenCount,
                       COALESCE(SUM(CASE WHEN expire_date BETWEEN CURDATE()
                         AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END), 0) AS expiringCount
                FROM wm_material_stock
                """));
        result.put("categories", jdbc.query("""
                SELECT COALESCE(t.item_type_id, 0) AS itemTypeId,
                       COALESCE(t.item_type_name, '未分类') AS itemTypeName,
                       COUNT(DISTINCT s.item_code) AS skuCount,
                       COALESCE(SUM(s.quantity_onhand), 0) AS quantityOnhand
                FROM wm_material_stock s
                LEFT JOIN md_item_type t ON t.item_type_id = s.item_type_id
                GROUP BY t.item_type_id, t.item_type_name
                ORDER BY quantityOnhand DESC
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemTypeId", rs.getObject("itemTypeId"));
            row.put("itemTypeName", rs.getString("itemTypeName"));
            row.put("skuCount", rs.getLong("skuCount"));
            row.put("quantityOnhand", rs.getBigDecimal("quantityOnhand"));
            return row;
        }));
        result.put("warehouses", jdbc.query("""
                SELECT warehouse_id AS warehouseId, warehouse_name AS warehouseName,
                       COUNT(DISTINCT item_code) AS skuCount,
                       COALESCE(SUM(quantity_onhand), 0) AS quantityOnhand
                FROM wm_material_stock
                GROUP BY warehouse_id, warehouse_name
                ORDER BY quantityOnhand DESC
                """, (rs, n) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("warehouseId", rs.getObject("warehouseId"));
            row.put("warehouseName", rs.getString("warehouseName"));
            row.put("skuCount", rs.getLong("skuCount"));
            row.put("quantityOnhand", rs.getBigDecimal("quantityOnhand"));
            return row;
        }));
        return result;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " AND s.material_stock_id = ?",
                (rs, n) -> mapRow(rs), id);
        return rows.stream().findFirst();
    }

    public Optional<Map<String, Object>> findBatchById(Long batchId) {
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT ib.batch_id AS batchId, ib.batch_no AS batchCode, ib.material_id AS itemId,
                       m.material_code AS itemCode, m.material_name AS itemName,
                       ib.available_qty AS quantityOnhand, ib.locked_qty AS quantityReserved,
                       ib.received_at AS recptDate, ib.expire_date AS expireDate
                FROM inventory_batch ib
                JOIN material m ON m.material_id = ib.material_id
                WHERE ib.batch_id = ?
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("batchId", rs.getLong("batchId"));
            m.put("batchCode", rs.getString("batchCode"));
            m.put("itemId", rs.getLong("itemId"));
            m.put("itemCode", rs.getString("itemCode"));
            m.put("itemName", rs.getString("itemName"));
            m.put("quantityOnhand", rs.getObject("quantityOnhand"));
            m.put("quantityReserved", rs.getObject("quantityReserved"));
            m.put("recptDate", rs.getTimestamp("recptDate"));
            m.put("expireDate", rs.getDate("expireDate"));
            return m;
        }, batchId);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO wm_material_stock (item_id, item_code, item_name, specification, unit_name,
                      batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
                      location_id, location_code, location_name, area_id, area_code, area_name,
                      quantity_onhand, quantity_reserved, frozen_flag, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, Long.parseLong(String.valueOf(body.get("itemId"))));
            ps.setString(i++, str(body, "itemCode"));
            ps.setString(i++, str(body, "itemName"));
            ps.setString(i++, str(body, "specification"));
            ps.setString(i++, str(body, "unitName"));
            setLong(ps, i++, body.get("batchId"));
            ps.setString(i++, str(body, "batchCode"));
            ps.setLong(i++, Long.parseLong(String.valueOf(body.get("warehouseId"))));
            ps.setString(i++, str(body, "warehouseCode"));
            ps.setString(i++, str(body, "warehouseName"));
            setLong(ps, i++, body.get("locationId"));
            ps.setString(i++, str(body, "locationCode"));
            ps.setString(i++, str(body, "locationName"));
            setLong(ps, i++, body.get("areaId"));
            ps.setString(i++, str(body, "areaCode"));
            ps.setString(i++, str(body, "areaName"));
            ps.setDouble(i++, doubleVal(body.get("quantityOnhand"), 0d));
            ps.setDouble(i++, doubleVal(body.get("quantityReserved"), 0d));
            ps.setString(i++, strOr(body, "frozenFlag", "N"));
            ps.setTimestamp(i, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Long id, Map<String, Object> body) {
        if (body.containsKey("frozenFlag") && body.size() <= 3) {
            return jdbc.update("UPDATE wm_material_stock SET frozen_flag = ?, update_time = ? WHERE material_stock_id = ?",
                    str(body, "frozenFlag"), Timestamp.valueOf(LocalDateTime.now()), id);
        }
        return jdbc.update("""
                UPDATE wm_material_stock SET quantity_onhand=?, quantity_reserved=?, remark=?, update_time=?
                WHERE material_stock_id=?
                """,
                doubleVal(body.get("quantityOnhand"), 0d),
                doubleVal(body.get("quantityReserved"), 0d),
                str(body, "remark"),
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM wm_material_stock WHERE material_stock_id = ?", id);
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> query) {
        if (StringUtils.hasText(query.get("itemId"))) {
            sql.append(" AND s.item_id = ?");
            args.add(Long.parseLong(query.get("itemId").trim()));
        }
        if (StringUtils.hasText(query.get("itemCode"))) {
            sql.append(" AND s.item_code LIKE ?");
            args.add("%" + query.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("itemName"))) {
            sql.append(" AND s.item_name LIKE ?");
            args.add("%" + query.get("itemName").trim() + "%");
        }
        if (StringUtils.hasText(query.get("batchCode"))) {
            sql.append(" AND s.batch_code LIKE ?");
            args.add("%" + query.get("batchCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("warehouseName"))) {
            sql.append(" AND s.warehouse_name LIKE ?");
            args.add("%" + query.get("warehouseName").trim() + "%");
        }
        if (StringUtils.hasText(query.get("warehouseId"))) {
            sql.append(" AND s.warehouse_id = ?");
            args.add(Long.parseLong(query.get("warehouseId").trim()));
        }
        if (StringUtils.hasText(query.get("locationName"))) {
            sql.append(" AND s.location_name LIKE ?");
            args.add("%" + query.get("locationName").trim() + "%");
        }
        if (StringUtils.hasText(query.get("areaCode"))) {
            sql.append(" AND s.area_code LIKE ?");
            args.add("%" + query.get("areaCode").trim() + "%");
        }
        if (StringUtils.hasText(query.get("frozenFlag"))) {
            sql.append(" AND s.frozen_flag = ?");
            args.add(query.get("frozenFlag").trim());
        }
        if ("Y".equals(query.get("nearExpiry"))) {
            sql.append(" AND s.expire_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
        }
        if (StringUtils.hasText(query.get("itemTypeId")) && !"0".equals(query.get("itemTypeId"))) {
            sql.append("""
                     AND s.item_type_id IN (
                       SELECT t.item_type_id
                       FROM md_item_type t
                       WHERE t.item_type_id = ?
                          OR FIND_IN_SET(CAST(? AS CHAR), t.ancestors)
                     )
                    """);
            Long itemTypeId = Long.parseLong(query.get("itemTypeId").trim());
            args.add(itemTypeId);
            args.add(itemTypeId);
        }
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialStockId", rs.getLong("material_stock_id"));
        m.put("itemTypeId", rs.getObject("item_type_id"));
        m.put("itemId", rs.getLong("item_id"));
        m.put("itemCode", rs.getString("item_code"));
        m.put("itemName", rs.getString("item_name"));
        m.put("specification", rs.getString("specification"));
        m.put("unitOfMeasure", rs.getString("unit_of_measure"));
        m.put("unitName", rs.getString("unit_name"));
        m.put("batchId", rs.getObject("batch_id"));
        m.put("batchCode", rs.getString("batch_code"));
        m.put("warehouseId", rs.getLong("warehouse_id"));
        m.put("warehouseCode", rs.getString("warehouse_code"));
        m.put("warehouseName", rs.getString("warehouse_name"));
        m.put("locationId", rs.getObject("location_id"));
        m.put("locationCode", rs.getString("location_code"));
        m.put("locationName", rs.getString("location_name"));
        m.put("areaId", rs.getObject("area_id"));
        m.put("areaCode", rs.getString("area_code"));
        m.put("areaName", rs.getString("area_name"));
        m.put("quantityOnhand", rs.getObject("quantity_onhand"));
        m.put("quantityReserved", rs.getObject("quantity_reserved"));
        m.put("productionDate", rs.getTimestamp("production_date"));
        m.put("recptDate", rs.getTimestamp("recpt_date"));
        m.put("expireDate", rs.getTimestamp("expire_date"));
        m.put("frozenFlag", rs.getString("frozen_flag"));
        m.put("remark", rs.getString("attr1"));
        m.put("createTime", rs.getTimestamp("create_time"));
        m.put("updateTime", rs.getTimestamp("update_time"));
        return m;
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String strOr(Map<String, Object> body, String key, String def) {
        String s = str(body, key);
        return s == null || s.isBlank() ? def : s;
    }

    private static double doubleVal(Object v, double def) {
        if (v == null || String.valueOf(v).isBlank()) {
            return def;
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static void setLong(PreparedStatement ps, int idx, Object v) throws SQLException {
        if (v == null || String.valueOf(v).isBlank()) {
            ps.setNull(idx, Types.BIGINT);
        } else {
            ps.setLong(idx, Long.parseLong(String.valueOf(v)));
        }
    }
}
