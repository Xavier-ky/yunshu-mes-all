package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import com.yunshu.mes.inventory.workflow.InventoryBatchBridgeService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmItemRecptService {

    private final JdbcTemplate jdbc;
    private final StorageCoreService storageCore;
    private final InventoryBatchBridgeService batchBridge;

    public WmItemRecptService(JdbcTemplate jdbc, StorageCoreService storageCore,
            InventoryBatchBridgeService batchBridge) {
        this.jdbc = jdbc;
        this.storageCore = storageCore;
        this.batchBridge = batchBridge;
    }

    public List<Map<String, Object>> listHeader(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_item_recpt", "recpt_id", WmDocSchemas.ITEM_RECPT,
                WmDocSchemas.filter(params, "recptCode", "recptName", "vendorName", "poCode", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long countHeader(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_item_recpt", WmDocSchemas.ITEM_RECPT,
                WmDocSchemas.filter(params, "recptCode", "recptName", "vendorName", "poCode", "status"));
    }

    public Map<String, Object> getHeader(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_item_recpt", "recpt_id", "recptId", WmDocSchemas.ITEM_RECPT, id);
    }

    @Transactional
    public Long createHeader(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_item_recpt", WmDocSchemas.ITEM_RECPT, body);
    }

    @Transactional
    public int updateHeader(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("recptId")));
        return WmSqlHelper.update(jdbc, "wm_item_recpt", "recpt_id", WmDocSchemas.ITEM_RECPT, body, id);
    }

    @Transactional
    public int deleteHeader(Long id) {
        Map<String, Object> h = getHeader(id);
        if (h != null && !"PREPARE".equals(h.get("status"))) {
            throw new IllegalArgumentException("只能删除草稿状态的单据!");
        }
        jdbc.update("DELETE FROM wm_item_recpt_detail WHERE recpt_id = ?", id);
        jdbc.update("DELETE FROM wm_item_recpt_line WHERE recpt_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_item_recpt", "recpt_id", id);
    }

    @Transactional
    public void confirm(Long recptId) {
        Long lines = jdbc.queryForObject("SELECT COUNT(*) FROM wm_item_recpt_line WHERE recpt_id = ?", Long.class, recptId);
        if (lines == null || lines == 0) {
            throw new IllegalArgumentException("请添加入库单行");
        }
        jdbc.update("UPDATE wm_item_recpt SET status = 'CONFIRMED', update_time = NOW() WHERE recpt_id = ?", recptId);
    }

    @Transactional
    public void execute(Long recptId) {
        Map<String, Object> h = getHeader(recptId);
        if (h == null) {
            throw new IllegalArgumentException("入库单不存在");
        }
        if (!"APPROVED".equals(h.get("status"))) {
            throw new IllegalArgumentException("只有待执行状态的单据才能执行入库");
        }
        storageCore.processItemRecpt(recptId);
        batchBridge.syncInboundFromItemRecpt(recptId);
        jdbc.update("UPDATE wm_item_recpt SET status = 'FINISHED', update_time = NOW() WHERE recpt_id = ?", recptId);
    }

    public List<Map<String, Object>> listLine(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_item_recpt_line", "line_id", WmDocSchemas.ITEM_RECPT_LINE,
                WmDocSchemas.filter(params, "recptId"), PageUtil.offset(pn, ps), ps);
    }

    public long countLine(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_item_recpt_line", WmDocSchemas.ITEM_RECPT_LINE,
                WmDocSchemas.filter(params, "recptId"));
    }

    public List<Map<String, Object>> listLineWithDetail(Map<String, String> params) {
        List<Map<String, Object>> lines = listLine(params);
        if (lines.isEmpty()) {
            return lines;
        }
        Long recptId = Long.parseLong(String.valueOf(lines.get(0).get("recptId")));
        List<Map<String, Object>> details = listDetail(Map.of("recptId", String.valueOf(recptId), "pageNum", "1", "pageSize", "1000"));
        Map<Long, List<Map<String, Object>>> byLine = details.stream()
                .collect(Collectors.groupingBy(d -> Long.parseLong(String.valueOf(d.get("lineId")))));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            line.put("details", byLine.getOrDefault(lineId, List.of()));
        }
        return lines;
    }

    public Map<String, Object> getLine(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_item_recpt_line", "line_id", "lineId", WmDocSchemas.ITEM_RECPT_LINE, id);
    }

    @Transactional
    public Long createLine(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_item_recpt_line", WmDocSchemas.ITEM_RECPT_LINE, body);
    }

    @Transactional
    public int updateLine(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return WmSqlHelper.update(jdbc, "wm_item_recpt_line", "line_id", WmDocSchemas.ITEM_RECPT_LINE, body, id);
    }

    @Transactional
    public int deleteLine(Long id) {
        jdbc.update("DELETE FROM wm_item_recpt_detail WHERE line_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_item_recpt_line", "line_id", id);
    }

    public List<Map<String, Object>> listDetail(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_item_recpt_detail", "detail_id", WmDocSchemas.ITEM_RECPT_DETAIL,
                WmDocSchemas.filter(params, "recptId", "lineId"), PageUtil.offset(pn, ps), ps);
    }

    public long countDetail(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_item_recpt_detail", WmDocSchemas.ITEM_RECPT_DETAIL,
                WmDocSchemas.filter(params, "recptId", "lineId"));
    }

    public Map<String, Object> getDetail(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_item_recpt_detail", "detail_id", "detailId", WmDocSchemas.ITEM_RECPT_DETAIL, id);
    }

    @Transactional
    public Long createDetail(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_item_recpt_detail", WmDocSchemas.ITEM_RECPT_DETAIL, body);
    }

    @Transactional
    public int updateDetail(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return WmSqlHelper.update(jdbc, "wm_item_recpt_detail", "detail_id", WmDocSchemas.ITEM_RECPT_DETAIL, body, id);
    }

    @Transactional
    public int deleteDetail(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_item_recpt_detail", "detail_id", id);
    }
}
