package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmProductSalesService {

    private final JdbcTemplate jdbc;
    private final StorageCoreService storageCore;

    public WmProductSalesService(JdbcTemplate jdbc, StorageCoreService storageCore) {
        this.jdbc = jdbc;
        this.storageCore = storageCore;
    }

    public List<Map<String, Object>> listHeader(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_sales", "sales_id", WmDocSchemas.PRODUCT_SALES,
                WmDocSchemas.filter(params, "salesCode", "salesName", "clientName", "soCode", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long countHeader(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_sales", WmDocSchemas.PRODUCT_SALES,
                WmDocSchemas.filter(params, "salesCode", "salesName", "clientName", "soCode", "status"));
    }

    public Map<String, Object> getHeader(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_sales", "sales_id", "salesId", WmDocSchemas.PRODUCT_SALES, id);
    }

    @Transactional
    public Long createHeader(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_product_sales", WmDocSchemas.PRODUCT_SALES, body);
    }

    @Transactional
    public int updateHeader(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("salesId")));
        return WmSqlHelper.update(jdbc, "wm_product_sales", "sales_id", WmDocSchemas.PRODUCT_SALES, body, id);
    }

    @Transactional
    public int deleteHeader(Long id) {
        Map<String, Object> h = getHeader(id);
        if (h != null && !"PREPARE".equals(h.get("status"))) {
            throw new IllegalArgumentException("只能删除草稿状态的单据!");
        }
        jdbc.update("DELETE FROM wm_product_sales_detail WHERE sales_id = ?", id);
        jdbc.update("DELETE FROM wm_product_sales_line WHERE sales_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_product_sales", "sales_id", id);
    }

    public boolean checkQuantity(Long salesId) {
        List<Map<String, Object>> lines = listLine(Map.of("salesId", String.valueOf(salesId), "pageNum", "1", "pageSize", "1000"));
        for (Map<String, Object> line : lines) {
            double sales = toDouble(line.get("quantitySales"));
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            Double picked = jdbc.queryForObject(
                    "SELECT IFNULL(SUM(quantity),0) FROM wm_product_sales_detail WHERE line_id = ?", Double.class, lineId);
            if (Math.abs(sales - (picked == null ? 0 : picked)) > 0.001) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    public void execute(Long salesId) {
        Map<String, Object> h = getHeader(salesId);
        if (h == null) {
            throw new IllegalArgumentException("销售出库单不存在");
        }
        if (!"APPROVED".equals(h.get("status"))) {
            throw new IllegalArgumentException("只有待执行状态的单据才能执行出库");
        }
        if (!checkQuantity(salesId)) {
            throw new IllegalArgumentException("拣货数量与出库数量不一致");
        }
        storageCore.processProductSales(salesId);
        jdbc.update("UPDATE wm_product_sales SET status = 'FINISHED', update_time = NOW() WHERE sales_id = ?", salesId);
    }

    public List<Map<String, Object>> listLine(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_sales_line", "line_id", WmDocSchemas.PRODUCT_SALES_LINE,
                WmDocSchemas.filter(params, "salesId"), PageUtil.offset(pn, ps), ps);
    }

    public long countLine(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_sales_line", WmDocSchemas.PRODUCT_SALES_LINE,
                WmDocSchemas.filter(params, "salesId"));
    }

    public List<Map<String, Object>> listLineWithDetail(Map<String, String> params) {
        List<Map<String, Object>> lines = listLine(params);
        if (lines.isEmpty()) {
            return lines;
        }
        Long salesId = Long.parseLong(String.valueOf(lines.get(0).get("salesId")));
        List<Map<String, Object>> details = listDetail(Map.of("salesId", String.valueOf(salesId), "pageNum", "1", "pageSize", "1000"));
        var byLine = details.stream().collect(Collectors.groupingBy(d -> Long.parseLong(String.valueOf(d.get("lineId")))));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            line.put("details", byLine.getOrDefault(lineId, List.of()));
        }
        return lines;
    }

    public Map<String, Object> getLine(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_sales_line", "line_id", "lineId", WmDocSchemas.PRODUCT_SALES_LINE, id);
    }

    @Transactional
    public Long createLine(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_product_sales_line", WmDocSchemas.PRODUCT_SALES_LINE, body);
    }

    @Transactional
    public int updateLine(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return WmSqlHelper.update(jdbc, "wm_product_sales_line", "line_id", WmDocSchemas.PRODUCT_SALES_LINE, body, id);
    }

    @Transactional
    public int deleteLine(Long id) {
        jdbc.update("DELETE FROM wm_product_sales_detail WHERE line_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_product_sales_line", "line_id", id);
    }

    public List<Map<String, Object>> listDetail(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_product_sales_detail", "detail_id", WmDocSchemas.PRODUCT_SALES_DETAIL,
                WmDocSchemas.filter(params, "salesId", "lineId"), PageUtil.offset(pn, ps), ps);
    }

    public long countDetail(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_product_sales_detail", WmDocSchemas.PRODUCT_SALES_DETAIL,
                WmDocSchemas.filter(params, "salesId", "lineId"));
    }

    public Map<String, Object> getDetail(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_product_sales_detail", "detail_id", "detailId", WmDocSchemas.PRODUCT_SALES_DETAIL, id);
    }

    @Transactional
    public Long createDetail(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_product_sales_detail", WmDocSchemas.PRODUCT_SALES_DETAIL, body);
    }

    @Transactional
    public int updateDetail(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return WmSqlHelper.update(jdbc, "wm_product_sales_detail", "detail_id", WmDocSchemas.PRODUCT_SALES_DETAIL, body, id);
    }

    @Transactional
    public int deleteDetail(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_product_sales_detail", "detail_id", id);
    }

    private static double toDouble(Object v) {
        return v == null ? 0 : Double.parseDouble(String.valueOf(v));
    }
}
