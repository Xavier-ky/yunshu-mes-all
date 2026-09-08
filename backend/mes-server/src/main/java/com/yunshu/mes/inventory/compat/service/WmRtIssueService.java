package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import com.yunshu.mes.inventory.workflow.InventoryBatchBridgeService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WmRtIssueService {

    private final JdbcTemplate jdbc;
    private final StorageCoreService storageCore;
    private final InventoryBatchBridgeService batchBridge;

    public WmRtIssueService(JdbcTemplate jdbc, StorageCoreService storageCore,
            InventoryBatchBridgeService batchBridge) {
        this.jdbc = jdbc;
        this.storageCore = storageCore;
        this.batchBridge = batchBridge;
    }

    public List<Map<String, Object>> listHeader(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_rt_issue", "rt_id", WmDocSchemas.RT_ISSUE,
                WmDocSchemas.filter(params, "rtCode", "rtName", "workorderCode", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long countHeader(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_rt_issue", WmDocSchemas.RT_ISSUE,
                WmDocSchemas.filter(params, "rtCode", "rtName", "workorderCode", "status"));
    }

    public Map<String, Object> getHeader(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_rt_issue", "rt_id", "rtId", WmDocSchemas.RT_ISSUE, id);
    }

    @Transactional
    public Long createHeader(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_rt_issue", WmDocSchemas.RT_ISSUE, body);
    }

    @Transactional
    public int updateHeader(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("rtId")));
        return WmSqlHelper.update(jdbc, "wm_rt_issue", "rt_id", WmDocSchemas.RT_ISSUE, body, id);
    }

    @Transactional
    public int deleteHeader(Long id) {
        jdbc.update("DELETE FROM wm_rt_issue_detail WHERE rt_id = ?", id);
        jdbc.update("DELETE FROM wm_rt_issue_line WHERE rt_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_rt_issue", "rt_id", id);
    }

    @Transactional
    public void execute(Long rtId) {
        Map<String, Object> h = getHeader(rtId);
        if (h == null) {
            throw new IllegalArgumentException("退料单不存在");
        }
        if (!"UNEXECUTE".equals(h.get("status"))) {
            throw new IllegalArgumentException("只有待执行状态的单据才能执行退料");
        }
        storageCore.processRtIssue(rtId);
        batchBridge.syncInboundFromRtIssue(rtId);
        jdbc.update("UPDATE wm_rt_issue SET status = 'FINISHED', update_time = NOW() WHERE rt_id = ?", rtId);
    }

    public List<Map<String, Object>> listLine(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_rt_issue_line", "line_id", WmDocSchemas.RT_ISSUE_LINE,
                WmDocSchemas.filter(params, "rtId"), PageUtil.offset(pn, ps), ps);
    }

    public long countLine(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_rt_issue_line", WmDocSchemas.RT_ISSUE_LINE,
                WmDocSchemas.filter(params, "rtId"));
    }

    public List<Map<String, Object>> listLineWithDetail(Map<String, String> params) {
        List<Map<String, Object>> lines = listLine(params);
        if (lines.isEmpty()) {
            return lines;
        }
        Long rtId = Long.parseLong(String.valueOf(lines.get(0).get("rtId")));
        List<Map<String, Object>> details = listDetail(Map.of("rtId", String.valueOf(rtId), "pageNum", "1", "pageSize", "1000"));
        var byLine = details.stream().collect(Collectors.groupingBy(d -> Long.parseLong(String.valueOf(d.get("lineId")))));
        for (Map<String, Object> line : lines) {
            Long lineId = Long.parseLong(String.valueOf(line.get("lineId")));
            line.put("details", byLine.getOrDefault(lineId, List.of()));
        }
        return lines;
    }

    public Map<String, Object> getLine(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_rt_issue_line", "line_id", "lineId", WmDocSchemas.RT_ISSUE_LINE, id);
    }

    @Transactional
    public Long createLine(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_rt_issue_line", WmDocSchemas.RT_ISSUE_LINE, body);
    }

    @Transactional
    public int updateLine(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("lineId")));
        return WmSqlHelper.update(jdbc, "wm_rt_issue_line", "line_id", WmDocSchemas.RT_ISSUE_LINE, body, id);
    }

    @Transactional
    public int deleteLine(Long id) {
        jdbc.update("DELETE FROM wm_rt_issue_detail WHERE line_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_rt_issue_line", "line_id", id);
    }

    public List<Map<String, Object>> listDetail(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_rt_issue_detail", "detail_id", WmDocSchemas.RT_ISSUE_DETAIL,
                WmDocSchemas.filter(params, "rtId", "lineId"), PageUtil.offset(pn, ps), ps);
    }

    public long countDetail(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_rt_issue_detail", WmDocSchemas.RT_ISSUE_DETAIL,
                WmDocSchemas.filter(params, "rtId", "lineId"));
    }

    public Map<String, Object> getDetail(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_rt_issue_detail", "detail_id", "detailId", WmDocSchemas.RT_ISSUE_DETAIL, id);
    }

    @Transactional
    public Long createDetail(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "wm_rt_issue_detail", WmDocSchemas.RT_ISSUE_DETAIL, body);
    }

    @Transactional
    public int updateDetail(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("detailId")));
        return WmSqlHelper.update(jdbc, "wm_rt_issue_detail", "detail_id", WmDocSchemas.RT_ISSUE_DETAIL, body, id);
    }

    @Transactional
    public int deleteDetail(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_rt_issue_detail", "detail_id", id);
    }
}
