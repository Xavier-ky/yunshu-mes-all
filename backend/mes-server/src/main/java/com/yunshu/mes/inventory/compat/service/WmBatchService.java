package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 批次主数据：映射 inventory_batch → WmBatch 字段；正/反向追溯。
 */
@Service
public class WmBatchService {

    private static final String BASE_SELECT = """
            SELECT ib.batch_id AS batchId,
                   ib.batch_no AS batchCode,
                   ib.material_id AS itemId,
                   m.material_code AS itemCode,
                   m.material_name AS itemName,
                   NULL AS specification,
                   u.unit_code AS unitOfMeasure,
                   u.unit_name AS unitName,
                   ib.received_at AS produceDate,
                   ib.expire_date AS expireDate,
                   ib.received_at AS recptDate,
                   ib.supplier_batch_no AS lotNumber,
                   CASE ib.quality_status
                     WHEN 'QUALIFIED' THEN 'OK'
                     WHEN 'UNQUALIFIED' THEN 'NG'
                     ELSE ib.quality_status
                   END AS qualityStatus,
                   ib.created_at AS createTime,
                   ib.updated_at AS updateTime,
                   'INVENTORY' AS batchSource
            FROM inventory_batch ib
            JOIN material m ON m.material_id = ib.material_id
            LEFT JOIN uom u ON u.unit_id = m.unit_id
            """;

    private static final String PRODUCT_BATCH_SELECT = """
            SELECT NULL AS batchId,
                   l.batch_code AS batchCode,
                   l.item_id AS itemId,
                   l.item_code AS itemCode,
                   l.item_name AS itemName,
                   l.specification AS specification,
                   'PCS' AS unitOfMeasure,
                   l.unit_name AS unitName,
                   MAX(r.recpt_date) AS produceDate,
                   NULL AS expireDate,
                   MAX(r.recpt_date) AS recptDate,
                   l.batch_code AS lotNumber,
                   'OK' AS qualityStatus,
                   MAX(l.create_time) AS createTime,
                   MAX(l.update_time) AS updateTime,
                   'PRODUCT_RECPT' AS batchSource
            FROM wm_product_recpt_line l
            JOIN wm_product_recpt r ON r.recpt_id = l.recpt_id
            WHERE l.batch_code IS NOT NULL AND l.batch_code <> ''
            """;

    private final JdbcTemplate jdbc;

    public WmBatchService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        StringBuilder sql = new StringBuilder("SELECT * FROM (")
                .append(BASE_SELECT)
                .append(" UNION ALL ")
                .append(PRODUCT_BATCH_SELECT)
                .append("""
                     AND NOT EXISTS (SELECT 1 FROM inventory_batch ib2 WHERE ib2.batch_no = l.batch_code)
                     GROUP BY l.batch_code, l.item_id, l.item_code, l.item_name, l.specification, l.unit_name
                    ) AS batch_union WHERE 1=1
                    """);
        List<Object> args = new ArrayList<>();
        appendUnionFilters(sql, args, params);
        sql.append(" ORDER BY createTime DESC LIMIT ? OFFSET ?");
        args.add(ps);
        args.add(PageUtil.offset(pn, ps));
        return jdbc.query(sql.toString(), (rs, n) -> mapBatch(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM (")
                .append(BASE_SELECT)
                .append(" UNION ALL ")
                .append(PRODUCT_BATCH_SELECT)
                .append("""
                     AND NOT EXISTS (SELECT 1 FROM inventory_batch ib2 WHERE ib2.batch_no = l.batch_code)
                     GROUP BY l.batch_code, l.item_id, l.item_code, l.item_name, l.specification, l.unit_name
                    ) AS batch_union WHERE 1=1
                    """);
        List<Object> args = new ArrayList<>();
        appendUnionFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> getById(Long batchId) {
        List<Map<String, Object>> rows = jdbc.query(BASE_SELECT + " WHERE ib.batch_id = ?",
                (rs, n) -> mapBatch(rs), batchId);
        return rows.stream().findFirst();
    }

    public List<Map<String, Object>> listForward(String batchCode) {
        if (!StringUtils.hasText(batchCode)) {
            return List.of();
        }
        return jdbc.query("""
                SELECT DISTINCT ic.workorder_code AS workorderCode,
                       ppl.item_id AS itemId, ppl.item_code AS itemCode, ppl.item_name AS itemName,
                       ppl.specification AS specification, ppl.unit_of_measure AS unitOfMeasure,
                       ppl.unit_name AS unitName, ppl.batch_id AS batchId, ppl.batch_code AS batchCode
                FROM wm_item_consume_detail icd
                JOIN wm_item_consume ic ON icd.record_id = ic.record_id
                LEFT JOIN wm_product_produce pp ON pp.workorder_id = ic.workorder_id
                LEFT JOIN wm_product_produce_line ppl ON pp.record_id = ppl.record_id
                WHERE icd.batch_code = ?
                UNION
                SELECT DISTINCT ic.workorder_code AS workorderCode,
                       l.item_id AS itemId, l.item_code AS itemCode, l.item_name AS itemName,
                       l.specification AS specification, 'PCS' AS unitOfMeasure,
                       l.unit_name AS unitName, NULL AS batchId, l.batch_code AS batchCode
                FROM wm_item_consume_detail icd
                JOIN wm_item_consume ic ON icd.record_id = ic.record_id
                JOIN wm_product_recpt r ON r.workorder_id = ic.workorder_id AND r.status = 'FINISHED'
                JOIN wm_product_recpt_line l ON l.recpt_id = r.recpt_id
                WHERE icd.batch_code = ?
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workorderCode", rs.getString("workorderCode"));
            m.put("itemId", rs.getObject("itemId"));
            m.put("itemCode", rs.getString("itemCode"));
            m.put("itemName", rs.getString("itemName"));
            m.put("specification", rs.getString("specification"));
            m.put("unitOfMeasure", rs.getString("unitOfMeasure"));
            m.put("unitName", rs.getString("unitName"));
            m.put("batchId", rs.getObject("batchId"));
            m.put("batchCode", rs.getString("batchCode"));
            return m;
        }, batchCode, batchCode);
    }

    public List<Map<String, Object>> listBackward(String batchCode) {
        if (!StringUtils.hasText(batchCode)) {
            return List.of();
        }
        return jdbc.query("""
                SELECT DISTINCT ic.workorder_code AS workorderCode,
                       icd.item_id AS itemId, icd.item_code AS itemCode, icd.item_name AS itemName,
                       icd.specification AS specification, icd.unit_of_measure AS unitOfMeasure,
                       icd.unit_name AS unitName, icd.batch_id AS batchId, icd.batch_code AS batchCode
                FROM wm_product_produce_line ppl
                JOIN wm_product_produce pp ON ppl.record_id = pp.record_id
                JOIN wm_item_consume ic ON pp.workorder_id = ic.workorder_id
                JOIN wm_item_consume_detail icd ON ic.record_id = icd.record_id
                WHERE ppl.batch_code = ?
                  AND icd.batch_code IS NOT NULL
                UNION
                SELECT DISTINCT ic.workorder_code AS workorderCode,
                       icd.item_id AS itemId, icd.item_code AS itemCode, icd.item_name AS itemName,
                       icd.specification AS specification, icd.unit_of_measure AS unitOfMeasure,
                       icd.unit_name AS unitName, icd.batch_id AS batchId, icd.batch_code AS batchCode
                FROM wm_product_recpt_line rl
                JOIN wm_product_recpt r ON rl.recpt_id = r.recpt_id
                JOIN wm_item_consume ic ON ic.workorder_id = r.workorder_id
                JOIN wm_item_consume_detail icd ON ic.record_id = icd.record_id
                WHERE rl.batch_code = ?
                  AND icd.batch_code IS NOT NULL
                """, (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workorderCode", rs.getString("workorderCode"));
            m.put("itemId", rs.getObject("itemId"));
            m.put("itemCode", rs.getString("itemCode"));
            m.put("itemName", rs.getString("itemName"));
            m.put("specification", rs.getString("specification"));
            m.put("unitOfMeasure", rs.getString("unitOfMeasure"));
            m.put("unitName", rs.getString("unitName"));
            m.put("batchId", rs.getObject("batchId"));
            m.put("batchCode", rs.getString("batchCode"));
            return m;
        }, batchCode, batchCode);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        Long itemId = longVal(body.get("itemId"));
        String batchCode = str(body.get("batchCode"));
        if (itemId == null || !StringUtils.hasText(batchCode)) {
            throw new IllegalArgumentException("物料与次次号不能为空");
        }
        Long warehouseId = longVal(body.get("warehouseId"));
        if (warehouseId == null) {
            warehouseId = jdbc.queryForObject("SELECT warehouse_id FROM warehouse ORDER BY warehouse_id LIMIT 1", Long.class);
        }
        String qs = toDbQuality(str(body.get("qualityStatus")));
        KeyHolder kh = new GeneratedKeyHolder();
        Long wh = warehouseId;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO inventory_batch (
                      material_id, warehouse_id, batch_no, supplier_batch_no,
                      available_qty, locked_qty, quality_status, received_at, expire_date, status)
                    VALUES (?,?,?,?,0,0,?,?,?,'IN_STOCK')
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, itemId);
            ps.setLong(i++, wh);
            ps.setString(i++, batchCode);
            ps.setString(i++, str(body.get("lotNumber")));
            ps.setString(i++, qs);
            Object recpt = body.get("recptDate");
            if (recpt != null && StringUtils.hasText(String.valueOf(recpt))) {
                ps.setTimestamp(i++, Timestamp.valueOf(String.valueOf(recpt).replace('T', ' ').substring(0, 19)));
            } else {
                ps.setTimestamp(i++, new Timestamp(System.currentTimeMillis()));
            }
            Object exp = body.get("expireDate");
            if (exp != null && StringUtils.hasText(String.valueOf(exp))) {
                ps.setDate(i++, java.sql.Date.valueOf(String.valueOf(exp).substring(0, 10)));
            } else {
                ps.setNull(i++, java.sql.Types.DATE);
            }
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long batchId = longVal(body.get("batchId"));
        if (batchId == null) {
            return 0;
        }
        return jdbc.update("""
                UPDATE inventory_batch SET
                  batch_no = COALESCE(?, batch_no),
                  supplier_batch_no = COALESCE(?, supplier_batch_no),
                  quality_status = COALESCE(?, quality_status),
                  expire_date = COALESCE(?, expire_date),
                  updated_at = NOW(3)
                WHERE batch_id = ?
                """,
                str(body.get("batchCode")),
                str(body.get("lotNumber")),
                toDbQuality(str(body.get("qualityStatus"))),
                body.get("expireDate") == null ? null : java.sql.Date.valueOf(String.valueOf(body.get("expireDate")).substring(0, 10)),
                batchId);
    }

    @Transactional
    public int delete(String ids) {
        int n = 0;
        for (String p : ids.split(",")) {
            n += jdbc.update("DELETE FROM inventory_batch WHERE batch_id = ?", Long.parseLong(p.trim()));
        }
        return n;
    }

    private void appendUnionFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        if (StringUtils.hasText(params.get("batchCode"))) {
            sql.append(" AND batchCode LIKE ?");
            args.add("%" + params.get("batchCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemCode"))) {
            sql.append(" AND itemCode LIKE ?");
            args.add("%" + params.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemName"))) {
            sql.append(" AND itemName LIKE ?");
            args.add("%" + params.get("itemName").trim() + "%");
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        if (!sql.toString().contains("WHERE")) {
            sql.append(" WHERE 1=1");
        }
        if (StringUtils.hasText(params.get("batchCode"))) {
            sql.append(" AND ib.batch_no LIKE ?");
            args.add("%" + params.get("batchCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemCode"))) {
            sql.append(" AND m.material_code LIKE ?");
            args.add("%" + params.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemName"))) {
            sql.append(" AND m.material_name LIKE ?");
            args.add("%" + params.get("itemName").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemId")) && !"0".equals(params.get("itemId"))) {
            sql.append(" AND ib.material_id = ?");
            args.add(Long.parseLong(params.get("itemId")));
        }
        if (StringUtils.hasText(params.get("qualityStatus"))) {
            sql.append(" AND ib.quality_status = ?");
            args.add(toDbQuality(params.get("qualityStatus")));
        }
    }

    private Map<String, Object> mapBatch(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("batchId", rs.getLong("batchId"));
        m.put("batchCode", rs.getString("batchCode"));
        m.put("itemId", rs.getObject("itemId"));
        m.put("itemCode", rs.getString("itemCode"));
        m.put("itemName", rs.getString("itemName"));
        m.put("specification", rs.getString("specification"));
        m.put("unitOfMeasure", rs.getString("unitOfMeasure"));
        m.put("unitName", rs.getString("unitName"));
        m.put("produceDate", rs.getTimestamp("produceDate"));
        m.put("expireDate", rs.getDate("expireDate"));
        m.put("recptDate", rs.getTimestamp("recptDate"));
        m.put("lotNumber", rs.getString("lotNumber"));
        m.put("qualityStatus", rs.getString("qualityStatus"));
        try {
            m.put("batchSource", rs.getString("batchSource"));
        } catch (java.sql.SQLException ignored) {
            m.put("batchSource", "INVENTORY");
        }
        m.put("createTime", rs.getTimestamp("createTime"));
        m.put("updateTime", rs.getTimestamp("updateTime"));
        return m;
    }

    private static String toDbQuality(String qs) {
        if (!StringUtils.hasText(qs)) {
            return "QUALIFIED";
        }
        return switch (qs) {
            case "OK", "QUALIFIED" -> "QUALIFIED";
            case "NG", "UNQUALIFIED" -> "UNQUALIFIED";
            default -> qs;
        };
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static Long longVal(Object o) {
        if (o == null || !StringUtils.hasText(String.valueOf(o))) {
            return null;
        }
        return Long.parseLong(String.valueOf(o));
    }
}
