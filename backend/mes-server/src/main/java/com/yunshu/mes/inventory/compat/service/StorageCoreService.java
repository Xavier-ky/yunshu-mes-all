package com.yunshu.mes.inventory.compat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageCoreService {

    private final JdbcTemplate jdbc;
    private final WmStockTxService stockTxService;

    public StorageCoreService(JdbcTemplate jdbc, WmStockTxService stockTxService) {
        this.jdbc = jdbc;
        this.stockTxService = stockTxService;
    }

    @Transactional
    public void processItemRecpt(Long recptId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, r.recpt_code FROM wm_item_recpt_detail d
                JOIN wm_item_recpt r ON r.recpt_id = d.recpt_id
                WHERE d.recpt_id = ?
                """, (rs, n) -> mapDetailRow(rs), recptId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的入库明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "ITEM_RECPT", "IR", recptId, d.get("recptCode"), d.get("lineId"));
            tx.put("transactionFlag", 1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    @Transactional
    public void processIssue(Long issueId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, h.issue_code FROM wm_issue_detail d
                JOIN wm_issue_header h ON h.issue_id = d.issue_id
                WHERE d.issue_id = ?
                """, (rs, n) -> mapIssueDetailRow(rs), issueId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的领料明细");
        }
        for (Map<String, Object> d : details) {
            enrichIssueDetailStock(d);
            Map<String, Object> tx = baseTx(d, "ITEM_ISSUE_OUT", "IS", issueId, d.get("issueCode"), d.get("lineId"));
            tx.put("materialStockId", d.get("materialStockId"));
            tx.put("transactionFlag", -1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    /** 拣货时若未写入 materialStockId/仓库，执行出库前按物料自动匹配库存行并回写明细。 */
    private void enrichIssueDetailStock(Map<String, Object> d) {
        if (d.get("materialStockId") != null) {
            return;
        }
        Long itemId = longObj(d.get("itemId"));
        if (itemId == null) {
            throw new IllegalArgumentException("拣货明细缺少物料信息");
        }
        double need = toDouble(d.get("quantity"));
        if (need <= 0) {
            throw new IllegalArgumentException("拣货数量必须大于0");
        }
        Long wh = longObj(d.get("warehouseId"));
        Long loc = longObj(d.get("locationId"));
        Long area = longObj(d.get("areaId"));
        String batch = str(d, "batchCode");

        List<Map<String, Object>> rows;
        if (wh != null) {
            rows = jdbc.query("""
                    SELECT material_stock_id, quantity_onhand, batch_id, batch_code,
                           warehouse_id, warehouse_code, warehouse_name,
                           location_id, location_code, location_name,
                           area_id, area_code, area_name, specification, unit_name
                    FROM wm_material_stock
                    WHERE item_id = ? AND warehouse_id = ?
                      AND IFNULL(location_id,0) = IFNULL(?,0)
                      AND IFNULL(area_id,0) = IFNULL(?,0)
                      AND IFNULL(batch_code,'') = IFNULL(?, '')
                      AND quantity_onhand >= ?
                    ORDER BY quantity_onhand DESC LIMIT 1
                    """, this::mapStockPickRow, itemId, wh, loc, area, batch, need);
        } else {
            rows = jdbc.query("""
                    SELECT material_stock_id, quantity_onhand, batch_id, batch_code,
                           warehouse_id, warehouse_code, warehouse_name,
                           location_id, location_code, location_name,
                           area_id, area_code, area_name, specification, unit_name
                    FROM wm_material_stock
                    WHERE item_id = ? AND quantity_onhand >= ?
                    ORDER BY quantity_onhand DESC LIMIT 1
                    """, this::mapStockPickRow, itemId, need);
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "物料 " + d.get("itemCode") + " 未关联有效库存或库存不足，请重新拣货并选择仓库批次");
        }
        Map<String, Object> stock = rows.get(0);
        d.put("materialStockId", stock.get("materialStockId"));
        if (d.get("batchId") == null) {
            d.put("batchId", stock.get("batchId"));
        }
        if (d.get("batchCode") == null) {
            d.put("batchCode", stock.get("batchCode"));
        }
        if (d.get("warehouseId") == null) {
            d.put("warehouseId", stock.get("warehouseId"));
            d.put("warehouseCode", stock.get("warehouseCode"));
            d.put("warehouseName", stock.get("warehouseName"));
        }
        if (d.get("locationId") == null) {
            d.put("locationId", stock.get("locationId"));
            d.put("locationCode", stock.get("locationCode"));
            d.put("locationName", stock.get("locationName"));
        }
        if (d.get("areaId") == null) {
            d.put("areaId", stock.get("areaId"));
            d.put("areaCode", stock.get("areaCode"));
            d.put("areaName", stock.get("areaName"));
        }
        if (d.get("specification") == null) {
            d.put("specification", stock.get("specification"));
        }
        if (d.get("unitName") == null) {
            d.put("unitName", stock.get("unitName"));
        }
        Long detailId = longObj(d.get("detailId"));
        if (detailId != null) {
            jdbc.update("""
                    UPDATE wm_issue_detail SET
                      material_stock_id = ?, batch_id = ?, batch_code = ?,
                      warehouse_id = ?, warehouse_code = ?, warehouse_name = ?,
                      location_id = ?, location_code = ?, location_name = ?,
                      area_id = ?, area_code = ?, area_name = ?,
                      update_time = NOW()
                    WHERE detail_id = ?
                    """,
                    stock.get("materialStockId"), stock.get("batchId"), stock.get("batchCode"),
                    stock.get("warehouseId"), stock.get("warehouseCode"), stock.get("warehouseName"),
                    stock.get("locationId"), stock.get("locationCode"), stock.get("locationName"),
                    stock.get("areaId"), stock.get("areaCode"), stock.get("areaName"),
                    detailId);
        }
    }

    private Map<String, Object> mapStockPickRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialStockId", rs.getLong("material_stock_id"));
        m.put("quantityOnhand", rs.getDouble("quantity_onhand"));
        m.put("batchId", rs.getObject("batch_id"));
        m.put("batchCode", rs.getString("batch_code"));
        m.put("warehouseId", rs.getObject("warehouse_id"));
        m.put("warehouseCode", rs.getString("warehouse_code"));
        m.put("warehouseName", rs.getString("warehouse_name"));
        m.put("locationId", rs.getObject("location_id"));
        m.put("locationCode", rs.getString("location_code"));
        m.put("locationName", rs.getString("location_name"));
        m.put("areaId", rs.getObject("area_id"));
        m.put("areaCode", rs.getString("area_code"));
        m.put("areaName", rs.getString("area_name"));
        m.put("specification", rs.getString("specification"));
        m.put("unitName", rs.getString("unit_name"));
        return m;
    }

    private static Long longObj(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static double toDouble(Object v) {
        return v == null ? 0 : Double.parseDouble(String.valueOf(v));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }

    @Transactional
    public void processRtIssue(Long rtId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, h.rt_code FROM wm_rt_issue_detail d
                JOIN wm_rt_issue h ON h.rt_id = d.rt_id
                WHERE d.rt_id = ?
                """, (rs, n) -> mapRtDetailRow(rs), rtId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的退料明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "ITEM_RT_ISSUE_IN", "RT", rtId, d.get("rtCode"), d.get("lineId"));
            tx.put("transactionFlag", 1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    @Transactional
    public void processProductRecpt(Long recptId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, r.recpt_code FROM wm_product_recpt_detail d
                JOIN wm_product_recpt r ON r.recpt_id = d.recpt_id
                WHERE d.recpt_id = ?
                """, (rs, n) -> mapDetailRow(rs), recptId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的产品入库明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "PRODUCT_RECPT_IN", "PRODUCT_RECPT", recptId, d.get("recptCode"), d.get("lineId"));
            tx.put("transactionFlag", 1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    @Transactional
    public void processRtVendor(Long rtId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, h.rt_code FROM wm_rt_vendor_detail d
                JOIN wm_rt_vendor h ON h.rt_id = d.rt_id
                WHERE d.rt_id = ?
                """, (rs, n) -> mapStockDetailRow(rs), rtId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的采购退货明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "ITEM_RTV", "RTV", rtId, d.get("rtCode"), d.get("lineId"));
            tx.put("materialStockId", d.get("materialStockId"));
            tx.put("transactionFlag", -1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    @Transactional
    public void processProductSales(Long salesId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, h.sales_code FROM wm_product_sales_detail d
                JOIN wm_product_sales h ON h.sales_id = d.sales_id
                WHERE d.sales_id = ?
                """, (rs, n) -> mapSalesDetailRow(rs), salesId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的销售出库明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "PRODUCT_SALES_OUT", "PRODUCT_SALES", salesId, d.get("salesCode"), d.get("lineId"));
            tx.put("materialStockId", d.get("materialStockId"));
            tx.put("transactionFlag", -1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    @Transactional
    public void processRtSales(Long rtId) {
        List<Map<String, Object>> details = jdbc.query("""
                SELECT d.*, h.rt_code FROM wm_rt_sales_detail d
                JOIN wm_rt_sales h ON h.rt_id = d.rt_id
                WHERE d.rt_id = ?
                """, (rs, n) -> mapRtSalesDetailRow(rs), rtId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("没有需要处理的销售退货明细");
        }
        for (Map<String, Object> d : details) {
            Map<String, Object> tx = baseTx(d, "RT_SALES_IN", "RT_SALES", rtId, d.get("rtCode"), d.get("lineId"));
            tx.put("transactionFlag", 1);
            tx.put("transactionQuantity", d.get("quantity"));
            stockTxService.processTransaction(tx);
        }
    }

    private Map<String, Object> baseTx(Map<String, Object> d, String type, String docType, Long docId, Object docCode, Object lineId) {
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("transactionType", type);
        tx.put("itemId", d.get("itemId"));
        tx.put("itemCode", d.get("itemCode"));
        tx.put("itemName", d.get("itemName"));
        tx.put("specification", d.get("specification"));
        tx.put("unitName", d.get("unitName"));
        tx.put("batchId", d.get("batchId"));
        tx.put("batchCode", d.get("batchCode"));
        tx.put("warehouseId", d.get("warehouseId"));
        tx.put("warehouseCode", d.get("warehouseCode"));
        tx.put("warehouseName", d.get("warehouseName"));
        tx.put("locationId", d.get("locationId"));
        tx.put("locationCode", d.get("locationCode"));
        tx.put("locationName", d.get("locationName"));
        tx.put("areaId", d.get("areaId"));
        tx.put("areaCode", d.get("areaCode"));
        tx.put("areaName", d.get("areaName"));
        tx.put("sourceDocType", docType);
        tx.put("sourceDocId", docId);
        tx.put("sourceDocCode", docCode);
        tx.put("sourceDocLineId", lineId);
        return tx;
    }

    private static Map<String, Object> mapStockLocationRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("lineId", rs.getObject("line_id"));
        m.put("itemId", rs.getLong("item_id"));
        m.put("itemCode", rs.getString("item_code"));
        m.put("itemName", rs.getString("item_name"));
        m.put("specification", rs.getString("specification"));
        m.put("unitName", rs.getString("unit_name"));
        m.put("quantity", rs.getDouble("quantity"));
        m.put("batchId", rs.getObject("batch_id"));
        m.put("batchCode", rs.getString("batch_code"));
        m.put("warehouseId", rs.getObject("warehouse_id"));
        m.put("warehouseCode", rs.getString("warehouse_code"));
        m.put("warehouseName", rs.getString("warehouse_name"));
        m.put("locationId", rs.getObject("location_id"));
        m.put("locationCode", rs.getString("location_code"));
        m.put("locationName", rs.getString("location_name"));
        m.put("areaId", rs.getObject("area_id"));
        m.put("areaCode", rs.getString("area_code"));
        m.put("areaName", rs.getString("area_name"));
        return m;
    }

    private static Map<String, Object> mapDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("recptCode", rs.getString("recpt_code"));
        return m;
    }

    private static Map<String, Object> mapIssueDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("detailId", rs.getObject("detail_id"));
        m.put("materialStockId", rs.getObject("material_stock_id"));
        m.put("issueCode", rs.getString("issue_code"));
        return m;
    }

    private static Map<String, Object> mapRtDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("materialStockId", rs.getObject("material_stock_id"));
        m.put("rtCode", rs.getString("rt_code"));
        return m;
    }

    private static Map<String, Object> mapStockDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("materialStockId", rs.getObject("material_stock_id"));
        m.put("rtCode", rs.getString("rt_code"));
        return m;
    }

    private static Map<String, Object> mapSalesDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("materialStockId", rs.getObject("material_stock_id"));
        m.put("salesCode", rs.getString("sales_code"));
        return m;
    }

    private static Map<String, Object> mapRtSalesDetailRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Map<String, Object> m = mapStockLocationRow(rs);
        m.put("rtCode", rs.getString("rt_code"));
        return m;
    }
}
